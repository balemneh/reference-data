package gov.dhs.cbp.reference.catalog.service;

import gov.dhs.cbp.reference.catalog.client.OpenMetadataClient;
import gov.dhs.cbp.reference.catalog.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for analyzing table schemas and suggesting mappings
 * to reference data types.
 */
@Service
public class SchemaAnalyzer {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaAnalyzer.class);
    
    // Pattern matchers for common column name patterns
    private static final Map<ReferenceDataType, List<Pattern>> COLUMN_PATTERNS = new HashMap<>();
    
    static {
        // Country patterns
        COLUMN_PATTERNS.put(ReferenceDataType.COUNTRY, Arrays.asList(
            Pattern.compile(".*country.*code.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*cntry.*c[do].*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*ctry.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*iso.*code.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*nation.*code.*", Pattern.CASE_INSENSITIVE)
        ));
        
    }
    
    // Required fields for each reference data type
    private static final Map<ReferenceDataType, Set<String>> REQUIRED_FIELDS = new HashMap<>();
    
    static {
        REQUIRED_FIELDS.put(ReferenceDataType.COUNTRY, 
            Set.of("country_code", "country_name"));
    }
    
    /**
     * Analyze table schema and suggest mapping to reference data
     */
    public TableMapping analyzeAndSuggestMapping(OpenMetadataClient.DatasetMetadata metadata,
                                                 ReferenceDataType referenceType) {
        log.info("Analyzing schema for table {} to map to {}", 
                metadata.getFullyQualifiedName(), referenceType);
        
        TableMapping mapping = new TableMapping();
        
        // Analyze each column
        for (OpenMetadataClient.Column column : metadata.getColumns()) {
            String suggestedField = suggestFieldMapping(column, referenceType);
            if (suggestedField != null) {
                mapping.addColumnMapping(column.getName(), suggestedField);
                
                // Set key column if it's a code field
                if (suggestedField.endsWith("_code") && mapping.getKeyColumn() == null) {
                    mapping.setKeyColumn(column.getName());
                }
            }
        }
        
        // Auto-detect code system if possible
        String codeSystem = detectCodeSystem(metadata, referenceType);
        if (codeSystem != null) {
            mapping.setCodeSystem(codeSystem);
        }
        
        log.info("Suggested {} column mappings for {}", 
                mapping.getColumnMappings().size(), metadata.getName());
        
        return mapping;
    }
    
    /**
     * Validate a mapping configuration
     */
    public ValidationResult validateMapping(OpenMetadataClient.DatasetMetadata metadata,
                                           ReferenceDataType referenceType,
                                           TableMapping mapping) {
        ValidationResult result = new ValidationResult();
        
        // Check if key column exists
        if (mapping.getKeyColumn() == null) {
            result.addError("keyColumn", "Key column is required for joining");
        } else {
            boolean keyColumnExists = metadata.getColumns().stream()
                .anyMatch(col -> col.getName().equals(mapping.getKeyColumn()));
            if (!keyColumnExists) {
                result.addError("keyColumn", 
                    "Key column '" + mapping.getKeyColumn() + "' not found in table");
            }
        }
        
        // Validate column mappings
        for (Map.Entry<String, String> entry : mapping.getColumnMappings().entrySet()) {
            String consumerColumn = entry.getKey();
            String referenceField = entry.getValue();
            
            // Check if consumer column exists
            boolean columnExists = metadata.getColumns().stream()
                .anyMatch(col -> col.getName().equals(consumerColumn));
            if (!columnExists) {
                result.addError("columnMapping", 
                    "Column '" + consumerColumn + "' not found in table");
            }
            
            // Check if reference field is valid
            if (!isValidReferenceField(referenceType, referenceField)) {
                result.addWarning("columnMapping",
                    "Field '" + referenceField + "' may not be a standard field for " + referenceType);
            }
        }
        
        // Check for required fields
        Set<String> requiredFields = REQUIRED_FIELDS.get(referenceType);
        if (requiredFields != null) {
            Set<String> mappedFields = new HashSet<>(mapping.getColumnMappings().values());
            Set<String> missingFields = requiredFields.stream()
                .filter(field -> !mappedFields.contains(field))
                .collect(Collectors.toSet());
            
            if (!missingFields.isEmpty()) {
                result.addWarning("requiredFields",
                    "Missing mappings for required fields: " + missingFields);
            }
        }
        
        // Calculate mapping score
        ValidationResult.MappingScore score = new ValidationResult.MappingScore();
        score.setMatchedFields(mapping.getColumnMappings().size());
        score.setTotalFields(metadata.getColumns().size());
        score.setHasRequiredFields(result.getWarnings().stream()
            .noneMatch(w -> w.getField().equals("requiredFields")));
        score.setConfidence(calculateConfidence(metadata, mapping, referenceType));
        result.setScore(score);
        
        return result;
    }
    
    /**
     * Suggest field mapping for a column based on name patterns
     */
    private String suggestFieldMapping(OpenMetadataClient.Column column, 
                                      ReferenceDataType referenceType) {
        String columnName = column.getName().toLowerCase();
        
        // Direct matches for common patterns
        Map<String, String> directMappings = getDirectMappings(referenceType);
        for (Map.Entry<String, String> entry : directMappings.entrySet()) {
            if (columnName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Pattern-based matching
        List<Pattern> patterns = COLUMN_PATTERNS.get(referenceType);
        if (patterns != null) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(columnName).matches()) {
                    return inferFieldName(columnName, referenceType);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get direct column name to field mappings
     */
    private Map<String, String> getDirectMappings(ReferenceDataType type) {
        Map<String, String> mappings = new HashMap<>();
        
        switch (type) {
            case COUNTRY:
                mappings.put("country_code", "country_code");
                mappings.put("country_name", "country_name");
                mappings.put("cntry_cd", "country_code");
                mappings.put("cntry_nm", "country_name");
                mappings.put("ctry_code", "country_code");
                mappings.put("iso_code", "iso3_code");
                mappings.put("iso2", "iso2_code");
                mappings.put("iso3", "iso3_code");
                break;
        }
        
        return mappings;
    }
    
    /**
     * Infer field name from column name
     */
    private String inferFieldName(String columnName, ReferenceDataType type) {
        // Remove common prefixes/suffixes
        String cleaned = columnName
            .replaceAll("^(rf_|ref_|dim_|lkp_)", "")
            .replaceAll("(_cd|_code|_id)$", "_code")
            .replaceAll("(_nm|_name|_desc)$", "_name");
        
        // Type-specific inference
        switch (type) {
            case COUNTRY:
                if (cleaned.contains("code")) return "country_code";
                if (cleaned.contains("name")) return "country_name";
                break;
        }
        
        return null;
    }
    
    /**
     * Detect code system from metadata
     */
    private String detectCodeSystem(OpenMetadataClient.DatasetMetadata metadata,
                                   ReferenceDataType type) {
        // Check custom properties first
        if (metadata.getCustomProperties() != null) {
            Object codeSystem = metadata.getCustomProperties().get("code_system");
            if (codeSystem != null) {
                return codeSystem.toString();
            }
        }
        
        // Infer from table name or description
        String tableName = metadata.getName().toLowerCase();
        switch (type) {
            case COUNTRY:
                if (tableName.contains("iso")) return "ISO3166-1";
                if (tableName.contains("genc")) return "GENC";
                if (tableName.contains("cbp")) return "CBP-COUNTRY5";
                break;
        }
        
        return null;
    }
    
    /**
     * Calculate confidence score for mapping
     */
    private double calculateConfidence(OpenMetadataClient.DatasetMetadata metadata,
                                      TableMapping mapping,
                                      ReferenceDataType type) {
        double score = 0.0;
        
        // Check key column mapping
        if (mapping.getKeyColumn() != null) {
            score += 0.3;
        }
        
        // Check required fields coverage
        Set<String> requiredFields = REQUIRED_FIELDS.get(type);
        if (requiredFields != null) {
            Set<String> mappedFields = new HashSet<>(mapping.getColumnMappings().values());
            long matchedRequired = requiredFields.stream()
                .filter(mappedFields::contains)
                .count();
            score += (0.4 * matchedRequired / requiredFields.size());
        }
        
        // Check column coverage
        double coverage = (double) mapping.getColumnMappings().size() / metadata.getColumns().size();
        score += (0.2 * Math.min(coverage, 1.0));
        
        // Check code system
        if (mapping.getCodeSystem() != null) {
            score += 0.1;
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * Check if a field name is valid for the reference type
     */
    private boolean isValidReferenceField(ReferenceDataType type, String field) {
        // This would check against a registry of valid fields per type
        // For now, basic validation
        return field != null && !field.isEmpty();
    }
}