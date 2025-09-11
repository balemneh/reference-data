package gov.dhs.cbp.reference.catalog.service;

import gov.dhs.cbp.reference.catalog.client.OpenMetadataClient;
import gov.dhs.cbp.reference.catalog.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for generating customized translation code based on
 * consumer table schemas and preferences.
 */
@Service
public class TranslationGeneratorService {
    
    private static final Logger log = LoggerFactory.getLogger(TranslationGeneratorService.class);
    
    private final OpenMetadataClient metadataClient;
    private final ConsumerRegistrationService registrationService;
    private final Map<String, TranslationStrategy> strategies = new HashMap<>();
    
    public TranslationGeneratorService(OpenMetadataClient metadataClient,
                                      ConsumerRegistrationService registrationService) {
        this.metadataClient = metadataClient;
        this.registrationService = registrationService;
        
        // Initialize translation strategies for supported databases
        strategies.put("postgres", new PostgresTranslationStrategy());
        strategies.put("postgresql", new PostgresTranslationStrategy());
        strategies.put("oracle", new OracleTranslationStrategy());
        strategies.put("db2", new DB2TranslationStrategy());
        strategies.put("mysql", new MySQLTranslationStrategy());
    }
    
    /**
     * Generate translation artifacts for a consumer registration
     */
    public Mono<TranslationArtifact> generateTranslation(ConsumerRegistration registration) {
        log.info("Generating translation for consumer {} table {}", 
                registration.getConsumerId(), registration.getTableFqn());
        
        return metadataClient.getDatasetMetadata(registration.getTableFqn())
                .map(metadata -> {
                    String engine = extractDatabaseEngine(registration.getTableFqn());
                    TranslationStrategy strategy = strategies.get(engine);
                    
                    if (strategy == null) {
                        strategy = strategies.get("postgres"); // Default
                    }
                    
                    return strategy.generateTranslation(
                        metadata,
                        registration.getReferenceDataType(),
                        registration.getMapping(),
                        registration.getPreferences()
                    );
                });
    }
    
    /**
     * Generate batch translation for all consumer registrations
     */
    public Mono<Map<String, TranslationArtifact>> generateBatchTranslations(String consumerId) {
        return registrationService.getConsumerRegistrations(consumerId)
                .flatMap(this::generateTranslation)
                .collectMap(
                    artifact -> artifact.getConsumerId() + ":" + artifact.getTableFqn(),
                    artifact -> artifact
                );
    }
    
    /**
     * Extract database engine from FQN
     */
    private String extractDatabaseEngine(String fqn) {
        // FQN format: engine.database.schema.table
        String[] parts = fqn.split("\\.");
        if (parts.length > 0) {
            return parts[0].toLowerCase();
        }
        return "postgres"; // Default
    }
    
    /**
     * Translation strategy interface
     */
    private interface TranslationStrategy {
        TranslationArtifact generateTranslation(
            OpenMetadataClient.DatasetMetadata metadata,
            ReferenceDataType referenceType,
            TableMapping mapping,
            TranslationPreferences preferences
        );
    }
    
    /**
     * PostgreSQL translation strategy
     */
    private static class PostgresTranslationStrategy implements TranslationStrategy {
        @Override
        public TranslationArtifact generateTranslation(
            OpenMetadataClient.DatasetMetadata metadata,
            ReferenceDataType referenceType,
            TableMapping mapping,
            TranslationPreferences preferences) {
            
            StringBuilder sql = new StringBuilder();
            String viewName = generateViewName(metadata.getName(), referenceType, preferences);
            
            // Generate appropriate artifact based on preference
            switch (preferences.getTranslationType()) {
                case VIEW:
                    sql.append(generateView(viewName, metadata, referenceType, mapping, preferences));
                    break;
                case MATERIALIZED_VIEW:
                    sql.append(generateMaterializedView(viewName, metadata, referenceType, mapping, preferences));
                    break;
                case UDF:
                    sql.append(generateUDF(viewName, metadata, referenceType, mapping, preferences));
                    break;
                case TABLE:
                    sql.append(generateMappingTable(viewName, metadata, referenceType, mapping, preferences));
                    break;
                default:
                    sql.append(generateView(viewName, metadata, referenceType, mapping, preferences));
            }
            
            TranslationArtifact artifact = new TranslationArtifact();
            artifact.setName(viewName);
            artifact.setType(preferences.getTranslationType().toString());
            artifact.setEngine("postgres");
            artifact.setContent(sql.toString());
            artifact.setGeneratedAt(LocalDateTime.now());
            artifact.setTableFqn(metadata.getFullyQualifiedName());
            artifact.setReferenceType(referenceType);
            
            return artifact;
        }
        
        private String generateView(String viewName, 
                                   OpenMetadataClient.DatasetMetadata metadata,
                                   ReferenceDataType referenceType,
                                   TableMapping mapping,
                                   TranslationPreferences preferences) {
            StringBuilder sql = new StringBuilder();
            
            sql.append("-- Translation view for ").append(referenceType).append(" reference data\n");
            sql.append("-- Generated for table: ").append(metadata.getFullyQualifiedName()).append("\n");
            sql.append("-- Generated at: ").append(LocalDateTime.now()).append("\n\n");
            
            sql.append("CREATE OR REPLACE VIEW ").append(viewName).append(" AS\n");
            sql.append("SELECT\n");
            
            // Select consumer columns
            sql.append("    t.*,\n");
            
            // Select mapped reference data columns
            for (Map.Entry<String, String> entry : mapping.getColumnMappings().entrySet()) {
                String refField = entry.getValue();
                sql.append("    r.").append(refField).append(" AS ref_").append(refField).append(",\n");
            }
            
            // Add metadata columns if requested
            if (preferences.isIncludeMetadata()) {
                sql.append("    r.valid_from AS ref_valid_from,\n");
                sql.append("    r.valid_to AS ref_valid_to,\n");
                sql.append("    r.is_active AS ref_is_active,\n");
            }
            
            // Remove trailing comma
            sql.setLength(sql.length() - 2);
            sql.append("\n");
            
            sql.append("FROM ").append(extractTableName(metadata)).append(" t\n");
            sql.append(mapping.getJoinType()).append(" JOIN reference_data.")
               .append(referenceType.getCode()).append("_current r\n");
            sql.append("    ON t.").append(mapping.getKeyColumn())
               .append(" = r.").append(referenceType.getCode()).append("_code\n");
            
            // Add code system filter if specified
            if (mapping.getCodeSystem() != null) {
                sql.append("    AND r.code_system = '").append(mapping.getCodeSystem()).append("'\n");
            }
            
            // Add filter condition if specified
            if (mapping.getFilterCondition() != null) {
                sql.append("WHERE ").append(mapping.getFilterCondition()).append("\n");
            }
            
            sql.append(";\n\n");
            
            // Add comment
            sql.append("COMMENT ON VIEW ").append(viewName).append(" IS\n");
            sql.append("    'Auto-generated translation view for ")
               .append(referenceType).append(" reference data';\n");
            
            return sql.toString();
        }
        
        private String generateMaterializedView(String viewName,
                                               OpenMetadataClient.DatasetMetadata metadata,
                                               ReferenceDataType referenceType,
                                               TableMapping mapping,
                                               TranslationPreferences preferences) {
            StringBuilder sql = new StringBuilder();
            
            // Generate base view SQL
            String viewSql = generateView(viewName + "_base", metadata, referenceType, mapping, preferences);
            
            sql.append("-- Materialized translation view\n");
            sql.append("CREATE MATERIALIZED VIEW ").append(viewName).append(" AS\n");
            sql.append("SELECT * FROM ").append(viewName).append("_base;\n\n");
            
            // Add indexes
            sql.append("-- Create indexes for performance\n");
            sql.append("CREATE INDEX idx_").append(viewName).append("_key\n");
            sql.append("    ON ").append(viewName).append(" (").append(mapping.getKeyColumn()).append(");\n\n");
            
            // Add refresh strategy
            if (preferences.getRefreshStrategy() == TranslationPreferences.RefreshStrategy.SCHEDULED) {
                sql.append("-- Refresh schedule (requires pg_cron or similar)\n");
                sql.append("-- SELECT cron.schedule('refresh_").append(viewName).append("',\n");
                sql.append("--     '*/").append(preferences.getRefreshIntervalMinutes()).append(" * * * *',\n");
                sql.append("--     'REFRESH MATERIALIZED VIEW CONCURRENTLY ").append(viewName).append("');\n");
            }
            
            return viewSql + sql.toString();
        }
        
        private String generateUDF(String functionName,
                                  OpenMetadataClient.DatasetMetadata metadata,
                                  ReferenceDataType referenceType,
                                  TableMapping mapping,
                                  TranslationPreferences preferences) {
            StringBuilder sql = new StringBuilder();
            
            sql.append("-- Translation function for ").append(referenceType).append("\n");
            sql.append("CREATE OR REPLACE FUNCTION ").append(functionName).append("(\n");
            sql.append("    p_code TEXT,\n");
            sql.append("    p_field TEXT DEFAULT 'name'\n");
            sql.append(") RETURNS TEXT AS $$\n");
            sql.append("DECLARE\n");
            sql.append("    v_result TEXT;\n");
            sql.append("BEGIN\n");
            sql.append("    SELECT\n");
            sql.append("        CASE p_field\n");
            
            // Add cases for each mapped field
            for (String refField : mapping.getColumnMappings().values()) {
                sql.append("            WHEN '").append(refField).append("' THEN ")
                   .append(refField).append("\n");
            }
            
            sql.append("            ELSE ").append(referenceType.getCode()).append("_name\n");
            sql.append("        END INTO v_result\n");
            sql.append("    FROM reference_data.").append(referenceType.getCode()).append("_current\n");
            sql.append("    WHERE ").append(referenceType.getCode()).append("_code = p_code\n");
            
            if (mapping.getCodeSystem() != null) {
                sql.append("        AND code_system = '").append(mapping.getCodeSystem()).append("'\n");
            }
            
            sql.append("    LIMIT 1;\n");
            sql.append("    \n");
            sql.append("    RETURN v_result;\n");
            sql.append("END;\n");
            sql.append("$$ LANGUAGE plpgsql IMMUTABLE;\n");
            
            return sql.toString();
        }
        
        private String generateMappingTable(String tableName,
                                           OpenMetadataClient.DatasetMetadata metadata,
                                           ReferenceDataType referenceType,
                                           TableMapping mapping,
                                           TranslationPreferences preferences) {
            StringBuilder sql = new StringBuilder();
            
            sql.append("-- Mapping table for ").append(referenceType).append("\n");
            sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n");
            sql.append("    consumer_code VARCHAR(50) PRIMARY KEY,\n");
            
            // Add columns for each mapped field
            for (String refField : mapping.getColumnMappings().values()) {
                sql.append("    ").append(refField).append(" VARCHAR(255),\n");
            }
            
            if (preferences.isIncludeMetadata()) {
                sql.append("    valid_from DATE,\n");
                sql.append("    valid_to DATE,\n");
                sql.append("    is_active BOOLEAN,\n");
            }
            
            sql.append("    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n");
            sql.append(");\n\n");
            
            // Insert data
            sql.append("-- Populate mapping table\n");
            sql.append("INSERT INTO ").append(tableName).append("\n");
            sql.append("SELECT DISTINCT\n");
            sql.append("    t.").append(mapping.getKeyColumn()).append(" AS consumer_code,\n");
            
            for (Map.Entry<String, String> entry : mapping.getColumnMappings().entrySet()) {
                String refField = entry.getValue();
                sql.append("    r.").append(refField).append(",\n");
            }
            
            if (preferences.isIncludeMetadata()) {
                sql.append("    r.valid_from,\n");
                sql.append("    r.valid_to,\n");
                sql.append("    r.is_active,\n");
            }
            
            sql.append("    CURRENT_TIMESTAMP\n");
            sql.append("FROM ").append(extractTableName(metadata)).append(" t\n");
            sql.append("LEFT JOIN reference_data.").append(referenceType.getCode()).append("_current r\n");
            sql.append("    ON t.").append(mapping.getKeyColumn())
               .append(" = r.").append(referenceType.getCode()).append("_code\n");
            
            if (mapping.getCodeSystem() != null) {
                sql.append("    AND r.code_system = '").append(mapping.getCodeSystem()).append("'\n");
            }
            
            sql.append("ON CONFLICT (consumer_code) DO UPDATE SET\n");
            
            for (String refField : mapping.getColumnMappings().values()) {
                sql.append("    ").append(refField).append(" = EXCLUDED.").append(refField).append(",\n");
            }
            
            sql.append("    last_updated = CURRENT_TIMESTAMP;\n");
            
            return sql.toString();
        }
        
        private String generateViewName(String tableName, 
                                       ReferenceDataType referenceType,
                                       TranslationPreferences preferences) {
            String base = tableName + "_" + referenceType.getCode() + "_trans";
            
            switch (preferences.getNamingConvention()) {
                case SNAKE_CASE:
                    return base.toLowerCase();
                case CAMEL_CASE:
                    return toCamelCase(base);
                case PASCAL_CASE:
                    return toPascalCase(base);
                case KEBAB_CASE:
                    return base.toLowerCase().replace("_", "-");
                default:
                    return base.toLowerCase();
            }
        }
        
        private String extractTableName(OpenMetadataClient.DatasetMetadata metadata) {
            String fqn = metadata.getFullyQualifiedName();
            String[] parts = fqn.split("\\.");
            if (parts.length >= 3) {
                // Return schema.table
                return parts[parts.length - 2] + "." + parts[parts.length - 1];
            }
            return metadata.getName();
        }
        
        private String toCamelCase(String input) {
            String[] parts = input.split("_");
            StringBuilder result = new StringBuilder(parts[0].toLowerCase());
            for (int i = 1; i < parts.length; i++) {
                result.append(parts[i].substring(0, 1).toUpperCase())
                      .append(parts[i].substring(1).toLowerCase());
            }
            return result.toString();
        }
        
        private String toPascalCase(String input) {
            String camel = toCamelCase(input);
            return camel.substring(0, 1).toUpperCase() + camel.substring(1);
        }
    }
    
    /**
     * Oracle translation strategy
     */
    private static class OracleTranslationStrategy implements TranslationStrategy {
        @Override
        public TranslationArtifact generateTranslation(
            OpenMetadataClient.DatasetMetadata metadata,
            ReferenceDataType referenceType,
            TableMapping mapping,
            TranslationPreferences preferences) {
            
            StringBuilder sql = new StringBuilder();
            String viewName = generateViewName(metadata.getName(), referenceType, preferences);
            
            switch (preferences.getTranslationType()) {
                case VIEW:
                    sql.append(generateOracleView(viewName, metadata, referenceType, mapping, preferences));
                    break;
                case MATERIALIZED_VIEW:
                    sql.append(generateOracleMaterializedView(viewName, metadata, referenceType, mapping, preferences));
                    break;
                case UDF:
                    sql.append(generateOracleFunction(viewName, metadata, referenceType, mapping, preferences));
                    break;
                case TABLE:
                    sql.append(generateOracleMappingTable(viewName, metadata, referenceType, mapping, preferences));
                    break;
                default:
                    sql.append(generateOracleView(viewName, metadata, referenceType, mapping, preferences));
            }
            
            TranslationArtifact artifact = new TranslationArtifact();
            artifact.setName(viewName);
            artifact.setType(preferences.getTranslationType().toString());
            artifact.setEngine("oracle");
            artifact.setContent(sql.toString());
            artifact.setGeneratedAt(LocalDateTime.now());
            artifact.setTableFqn(metadata.getFullyQualifiedName());
            artifact.setReferenceType(referenceType);
            
            return artifact;
        }
        
        private String generateOracleView(String viewName, 
                                         OpenMetadataClient.DatasetMetadata metadata,
                                         ReferenceDataType referenceType,
                                         TableMapping mapping,
                                         TranslationPreferences preferences) {
            StringBuilder sql = new StringBuilder();
            
            sql.append("-- Oracle translation view for ").append(referenceType).append("\n");
            sql.append("CREATE OR REPLACE VIEW ").append(viewName).append(" AS\n");
            sql.append("SELECT\n");
            sql.append("    t.*,\n");
            
            for (Map.Entry<String, String> entry : mapping.getColumnMappings().entrySet()) {
                String refField = entry.getValue();
                sql.append("    r.").append(refField).append(" AS ref_").append(refField).append(",\n");
            }
            
            if (preferences.isIncludeMetadata()) {
                sql.append("    r.valid_from AS ref_valid_from,\n");
                sql.append("    r.valid_to AS ref_valid_to,\n");
                sql.append("    r.is_active AS ref_is_active,\n");
            }
            
            sql.setLength(sql.length() - 2);
            sql.append("\n");
            
            sql.append("FROM ").append(extractTableName(metadata)).append(" t\n");
            
            // Oracle uses (+) for outer joins in WHERE clause or standard JOIN syntax
            if (mapping.getJoinType() == TableMapping.JoinType.LEFT) {
                sql.append("LEFT OUTER JOIN ");
            } else {
                sql.append(mapping.getJoinType()).append(" JOIN ");
            }
            
            sql.append("reference_data.").append(referenceType.getCode()).append("_current r\n");
            sql.append("    ON t.").append(mapping.getKeyColumn())
               .append(" = r.").append(referenceType.getCode()).append("_code\n");
            
            if (mapping.getCodeSystem() != null) {
                sql.append("    AND r.code_system = '").append(mapping.getCodeSystem()).append("'\n");
            }
            
            if (mapping.getFilterCondition() != null) {
                sql.append("WHERE ").append(mapping.getFilterCondition()).append("\n");
            }
            
            sql.append(";\n\n");
            
            sql.append("COMMENT ON VIEW ").append(viewName).append(" IS\n");
            sql.append("    'Auto-generated translation view for ").append(referenceType).append("';\n");
            
            return sql.toString();
        }
        
        private String generateOracleMaterializedView(String viewName,
                                                     OpenMetadataClient.DatasetMetadata metadata,
                                                     ReferenceDataType referenceType,
                                                     TableMapping mapping,
                                                     TranslationPreferences preferences) {
            StringBuilder sql = new StringBuilder();
            
            sql.append("-- Oracle materialized view\n");
            sql.append("CREATE MATERIALIZED VIEW ").append(viewName).append("\n");
            
            if (preferences.getRefreshStrategy() == TranslationPreferences.RefreshStrategy.REAL_TIME) {
                sql.append("REFRESH FAST ON COMMIT\n");
            } else if (preferences.getRefreshStrategy() == TranslationPreferences.RefreshStrategy.SCHEDULED) {
                sql.append("REFRESH COMPLETE\n");
                sql.append("START WITH SYSDATE\n");
                sql.append("NEXT SYSDATE + ").append(preferences.getRefreshIntervalMinutes()).append("/1440\n");
            } else {
                sql.append("REFRESH ON DEMAND\n");
            }
            
            sql.append("AS\n");
            sql.append(generateOracleView(viewName + "_base", metadata, referenceType, mapping, preferences)
                    .replace("CREATE OR REPLACE VIEW " + viewName + "_base AS", ""));
            
            return sql.toString();
        }
        
        private String generateOracleFunction(String functionName,
                                             OpenMetadataClient.DatasetMetadata metadata,
                                             ReferenceDataType referenceType,
                                             TableMapping mapping,
                                             TranslationPreferences preferences) {
            StringBuilder sql = new StringBuilder();
            
            sql.append("-- Oracle translation function\n");
            sql.append("CREATE OR REPLACE FUNCTION ").append(functionName).append("(\n");
            sql.append("    p_code VARCHAR2,\n");
            sql.append("    p_field VARCHAR2 DEFAULT 'name'\n");
            sql.append(") RETURN VARCHAR2 IS\n");
            sql.append("    v_result VARCHAR2(4000);\n");
            sql.append("BEGIN\n");
            sql.append("    SELECT\n");
            sql.append("        CASE p_field\n");
            
            for (String refField : mapping.getColumnMappings().values()) {
                sql.append("            WHEN '").append(refField).append("' THEN ")
                   .append(refField).append("\n");
            }
            
            sql.append("            ELSE ").append(referenceType.getCode()).append("_name\n");
            sql.append("        END INTO v_result\n");
            sql.append("    FROM reference_data.").append(referenceType.getCode()).append("_current\n");
            sql.append("    WHERE ").append(referenceType.getCode()).append("_code = p_code\n");
            
            if (mapping.getCodeSystem() != null) {
                sql.append("        AND code_system = '").append(mapping.getCodeSystem()).append("'\n");
            }
            
            sql.append("        AND ROWNUM = 1;\n");
            sql.append("    \n");
            sql.append("    RETURN v_result;\n");
            sql.append("EXCEPTION\n");
            sql.append("    WHEN NO_DATA_FOUND THEN\n");
            sql.append("        RETURN NULL;\n");
            sql.append("END ").append(functionName).append(";\n");
            sql.append("/\n");
            
            return sql.toString();
        }
        
        private String generateOracleMappingTable(String tableName,
                                                 OpenMetadataClient.DatasetMetadata metadata,
                                                 ReferenceDataType referenceType,
                                                 TableMapping mapping,
                                                 TranslationPreferences preferences) {
            StringBuilder sql = new StringBuilder();
            
            sql.append("-- Oracle mapping table\n");
            sql.append("CREATE TABLE ").append(tableName).append(" (\n");
            sql.append("    consumer_code VARCHAR2(50) PRIMARY KEY,\n");
            
            for (String refField : mapping.getColumnMappings().values()) {
                sql.append("    ").append(refField).append(" VARCHAR2(255),\n");
            }
            
            if (preferences.isIncludeMetadata()) {
                sql.append("    valid_from DATE,\n");
                sql.append("    valid_to DATE,\n");
                sql.append("    is_active CHAR(1),\n");
            }
            
            sql.append("    last_updated TIMESTAMP DEFAULT SYSTIMESTAMP\n");
            sql.append(");\n\n");
            
            // Create merge statement for upsert
            sql.append("-- Populate mapping table\n");
            sql.append("MERGE INTO ").append(tableName).append(" tgt\n");
            sql.append("USING (\n");
            sql.append("    SELECT DISTINCT\n");
            sql.append("        t.").append(mapping.getKeyColumn()).append(" AS consumer_code,\n");
            
            for (Map.Entry<String, String> entry : mapping.getColumnMappings().entrySet()) {
                String refField = entry.getValue();
                sql.append("        r.").append(refField).append(",\n");
            }
            
            if (preferences.isIncludeMetadata()) {
                sql.append("        r.valid_from,\n");
                sql.append("        r.valid_to,\n");
                sql.append("        r.is_active,\n");
            }
            
            sql.append("        SYSTIMESTAMP AS last_updated\n");
            sql.append("    FROM ").append(extractTableName(metadata)).append(" t\n");
            sql.append("    LEFT JOIN reference_data.").append(referenceType.getCode()).append("_current r\n");
            sql.append("        ON t.").append(mapping.getKeyColumn())
               .append(" = r.").append(referenceType.getCode()).append("_code\n");
            
            if (mapping.getCodeSystem() != null) {
                sql.append("        AND r.code_system = '").append(mapping.getCodeSystem()).append("'\n");
            }
            
            sql.append(") src\n");
            sql.append("ON (tgt.consumer_code = src.consumer_code)\n");
            sql.append("WHEN MATCHED THEN UPDATE SET\n");
            
            for (String refField : mapping.getColumnMappings().values()) {
                sql.append("    tgt.").append(refField).append(" = src.").append(refField).append(",\n");
            }
            
            sql.append("    tgt.last_updated = SYSTIMESTAMP\n");
            sql.append("WHEN NOT MATCHED THEN INSERT VALUES (\n");
            sql.append("    src.consumer_code");
            
            for (String refField : mapping.getColumnMappings().values()) {
                sql.append(",\n    src.").append(refField);
            }
            
            if (preferences.isIncludeMetadata()) {
                sql.append(",\n    src.valid_from");
                sql.append(",\n    src.valid_to");
                sql.append(",\n    src.is_active");
            }
            
            sql.append(",\n    SYSTIMESTAMP\n");
            sql.append(");\n");
            
            return sql.toString();
        }
        
        private String generateViewName(String tableName, 
                                       ReferenceDataType referenceType,
                                       TranslationPreferences preferences) {
            String base = tableName + "_" + referenceType.getCode() + "_trans";
            return base.toUpperCase(); // Oracle traditionally uses uppercase
        }
        
        private String extractTableName(OpenMetadataClient.DatasetMetadata metadata) {
            String fqn = metadata.getFullyQualifiedName();
            String[] parts = fqn.split("\\.");
            if (parts.length >= 3) {
                return parts[parts.length - 2] + "." + parts[parts.length - 1];
            }
            return metadata.getName();
        }
    }
    
    /**
     * DB2 translation strategy
     */
    private static class DB2TranslationStrategy implements TranslationStrategy {
        @Override
        public TranslationArtifact generateTranslation(
            OpenMetadataClient.DatasetMetadata metadata,
            ReferenceDataType referenceType,
            TableMapping mapping,
            TranslationPreferences preferences) {
            
            StringBuilder sql = new StringBuilder();
            String viewName = generateViewName(metadata.getName(), referenceType, preferences);
            
            // DB2 uses similar syntax to standard SQL
            sql.append("-- DB2 translation view for ").append(referenceType).append("\n");
            sql.append("CREATE OR REPLACE VIEW ").append(viewName).append(" AS\n");
            sql.append("SELECT\n");
            sql.append("    t.*,\n");
            
            for (Map.Entry<String, String> entry : mapping.getColumnMappings().entrySet()) {
                String refField = entry.getValue();
                sql.append("    r.").append(refField).append(" AS ref_").append(refField).append(",\n");
            }
            
            if (preferences.isIncludeMetadata()) {
                sql.append("    r.valid_from AS ref_valid_from,\n");
                sql.append("    r.valid_to AS ref_valid_to,\n");
                sql.append("    r.is_active AS ref_is_active,\n");
            }
            
            sql.setLength(sql.length() - 2);
            sql.append("\n");
            
            sql.append("FROM ").append(extractTableName(metadata)).append(" t\n");
            sql.append(mapping.getJoinType()).append(" JOIN reference_data.")
               .append(referenceType.getCode()).append("_current r\n");
            sql.append("    ON t.").append(mapping.getKeyColumn())
               .append(" = r.").append(referenceType.getCode()).append("_code\n");
            
            if (mapping.getCodeSystem() != null) {
                sql.append("    AND r.code_system = '").append(mapping.getCodeSystem()).append("'\n");
            }
            
            if (mapping.getFilterCondition() != null) {
                sql.append("WHERE ").append(mapping.getFilterCondition()).append("\n");
            }
            
            sql.append(";\n\n");
            
            sql.append("COMMENT ON VIEW ").append(viewName).append(" IS\n");
            sql.append("    'Auto-generated translation view for ").append(referenceType).append("';\n");
            
            TranslationArtifact artifact = new TranslationArtifact();
            artifact.setName(viewName);
            artifact.setType(preferences.getTranslationType().toString());
            artifact.setEngine("db2");
            artifact.setContent(sql.toString());
            artifact.setGeneratedAt(LocalDateTime.now());
            artifact.setTableFqn(metadata.getFullyQualifiedName());
            artifact.setReferenceType(referenceType);
            
            return artifact;
        }
        
        private String generateViewName(String tableName, 
                                       ReferenceDataType referenceType,
                                       TranslationPreferences preferences) {
            String base = tableName + "_" + referenceType.getCode() + "_trans";
            return base.toUpperCase(); // DB2 traditionally uses uppercase
        }
        
        private String extractTableName(OpenMetadataClient.DatasetMetadata metadata) {
            String fqn = metadata.getFullyQualifiedName();
            String[] parts = fqn.split("\\.");
            if (parts.length >= 3) {
                return parts[parts.length - 2] + "." + parts[parts.length - 1];
            }
            return metadata.getName();
        }
    }
    
    /**
     * MySQL translation strategy
     */
    private static class MySQLTranslationStrategy implements TranslationStrategy {
        @Override
        public TranslationArtifact generateTranslation(
            OpenMetadataClient.DatasetMetadata metadata,
            ReferenceDataType referenceType,
            TableMapping mapping,
            TranslationPreferences preferences) {
            
            StringBuilder sql = new StringBuilder();
            String viewName = generateViewName(metadata.getName(), referenceType, preferences);
            
            // MySQL view syntax
            sql.append("-- MySQL translation view for ").append(referenceType).append("\n");
            sql.append("CREATE OR REPLACE VIEW ").append(viewName).append(" AS\n");
            sql.append("SELECT\n");
            sql.append("    t.*,\n");
            
            for (Map.Entry<String, String> entry : mapping.getColumnMappings().entrySet()) {
                String refField = entry.getValue();
                sql.append("    r.").append(refField).append(" AS ref_").append(refField).append(",\n");
            }
            
            if (preferences.isIncludeMetadata()) {
                sql.append("    r.valid_from AS ref_valid_from,\n");
                sql.append("    r.valid_to AS ref_valid_to,\n");
                sql.append("    r.is_active AS ref_is_active,\n");
            }
            
            sql.setLength(sql.length() - 2);
            sql.append("\n");
            
            sql.append("FROM ").append(extractTableName(metadata)).append(" t\n");
            sql.append(mapping.getJoinType()).append(" JOIN reference_data.")
               .append(referenceType.getCode()).append("_current r\n");
            sql.append("    ON t.").append(mapping.getKeyColumn())
               .append(" = r.").append(referenceType.getCode()).append("_code\n");
            
            if (mapping.getCodeSystem() != null) {
                sql.append("    AND r.code_system = '").append(mapping.getCodeSystem()).append("'\n");
            }
            
            if (mapping.getFilterCondition() != null) {
                sql.append("WHERE ").append(mapping.getFilterCondition()).append("\n");
            }
            
            sql.append(";\n");
            
            TranslationArtifact artifact = new TranslationArtifact();
            artifact.setName(viewName);
            artifact.setType(preferences.getTranslationType().toString());
            artifact.setEngine("mysql");
            artifact.setContent(sql.toString());
            artifact.setGeneratedAt(LocalDateTime.now());
            artifact.setTableFqn(metadata.getFullyQualifiedName());
            artifact.setReferenceType(referenceType);
            
            return artifact;
        }
        
        private String generateViewName(String tableName, 
                                       ReferenceDataType referenceType,
                                       TranslationPreferences preferences) {
            String base = tableName + "_" + referenceType.getCode() + "_trans";
            return base.toLowerCase(); // MySQL uses lowercase by default
        }
        
        private String extractTableName(OpenMetadataClient.DatasetMetadata metadata) {
            String fqn = metadata.getFullyQualifiedName();
            String[] parts = fqn.split("\\.");
            if (parts.length >= 3) {
                return parts[parts.length - 2] + "." + parts[parts.length - 1];
            }
            return metadata.getName();
        }
    }
}