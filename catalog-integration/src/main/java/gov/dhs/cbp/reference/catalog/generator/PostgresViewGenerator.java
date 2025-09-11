package gov.dhs.cbp.reference.catalog.generator;

import gov.dhs.cbp.reference.catalog.client.OpenMetadataClient.Column;
import gov.dhs.cbp.reference.catalog.client.OpenMetadataClient.DatasetMetadata;

import java.util.stream.Collectors;

public class PostgresViewGenerator implements CodeGenerator {
    
    @Override
    public String generate(DatasetMetadata dataset) {
        StringBuilder sql = new StringBuilder();
        
        // Generate view name
        String viewName = "v_" + dataset.getName().toLowerCase();
        
        sql.append("-- Generated view for ").append(dataset.getName()).append("\n");
        sql.append("-- Source: ").append(dataset.getFullyQualifiedName()).append("\n");
        if (dataset.getDescription() != null) {
            sql.append("-- Description: ").append(dataset.getDescription()).append("\n");
        }
        sql.append("\n");
        
        sql.append("CREATE OR REPLACE VIEW ").append(viewName).append(" AS\n");
        sql.append("SELECT\n");
        
        // Generate column list
        String columns = dataset.getColumns().stream()
                .map(this::generateColumnSelect)
                .collect(Collectors.joining(",\n    "));
        sql.append("    ").append(columns).append("\n");
        
        // Determine source table
        String sourceTable = determineSourceTable(dataset);
        sql.append("FROM ").append(sourceTable).append("\n");
        
        // Add bitemporal filter for current data
        if (isBitemporalDataset(dataset)) {
            sql.append("WHERE (valid_to IS NULL OR valid_to > CURRENT_DATE)\n");
            sql.append("  AND is_active = true");
        }
        
        sql.append(";\n\n");
        
        // Add comment on view
        sql.append("COMMENT ON VIEW ").append(viewName).append(" IS '");
        sql.append("Reference data view for ").append(dataset.getName());
        if (dataset.getDescription() != null) {
            sql.append(". ").append(dataset.getDescription().replace("'", "''"));
        }
        sql.append("';\n");
        
        // Add column comments
        for (Column column : dataset.getColumns()) {
            if (column.getDescription() != null) {
                sql.append("COMMENT ON COLUMN ").append(viewName).append(".")
                   .append(column.getName().toLowerCase()).append(" IS '")
                   .append(column.getDescription().replace("'", "''")).append("';\n");
            }
        }
        
        return sql.toString();
    }
    
    private String generateColumnSelect(Column column) {
        String columnName = column.getName().toLowerCase();
        
        // Handle special column transformations
        if ("metadata".equals(columnName)) {
            return columnName + "::jsonb AS " + columnName;
        } else if (columnName.endsWith("_at") || columnName.endsWith("_date")) {
            return columnName + "::timestamp AS " + columnName;
        } else {
            return columnName;
        }
    }
    
    private String determineSourceTable(DatasetMetadata dataset) {
        String name = dataset.getName().toLowerCase();
        
        // Check if it's a versioned table
        if (name.endsWith("_v") || name.endsWith("_version")) {
            return "reference_data." + name;
        } else {
            // Assume it's a current view or table
            return "reference_data." + name + "_current";
        }
    }
    
    private boolean isBitemporalDataset(DatasetMetadata dataset) {
        return dataset.getColumns().stream()
                .anyMatch(col -> "valid_from".equals(col.getName().toLowerCase()) ||
                                "valid_to".equals(col.getName().toLowerCase()));
    }
    
    @Override
    public String getEngineType() {
        return "postgres";
    }
}