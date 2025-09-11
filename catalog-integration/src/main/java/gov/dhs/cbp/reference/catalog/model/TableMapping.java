package gov.dhs.cbp.reference.catalog.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the mapping between consumer table columns
 * and reference data fields.
 */
public class TableMapping {
    
    @JsonProperty("column_mappings")
    private Map<String, String> columnMappings = new HashMap<>();
    
    @JsonProperty("key_column")
    private String keyColumn;
    
    @JsonProperty("code_system")
    private String codeSystem;
    
    @JsonProperty("transformation_rules")
    private Map<String, TransformationRule> transformationRules = new HashMap<>();
    
    @JsonProperty("filter_condition")
    private String filterCondition;
    
    @JsonProperty("join_type")
    private JoinType joinType = JoinType.LEFT;
    
    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }
    
    public void setColumnMappings(Map<String, String> columnMappings) {
        this.columnMappings = columnMappings;
    }
    
    public void addColumnMapping(String consumerColumn, String referenceField) {
        this.columnMappings.put(consumerColumn, referenceField);
    }
    
    public String getKeyColumn() {
        return keyColumn;
    }
    
    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }
    
    public String getCodeSystem() {
        return codeSystem;
    }
    
    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }
    
    public Map<String, TransformationRule> getTransformationRules() {
        return transformationRules;
    }
    
    public void setTransformationRules(Map<String, TransformationRule> transformationRules) {
        this.transformationRules = transformationRules;
    }
    
    public void addTransformationRule(String column, TransformationRule rule) {
        this.transformationRules.put(column, rule);
    }
    
    public String getFilterCondition() {
        return filterCondition;
    }
    
    public void setFilterCondition(String filterCondition) {
        this.filterCondition = filterCondition;
    }
    
    public JoinType getJoinType() {
        return joinType;
    }
    
    public void setJoinType(JoinType joinType) {
        this.joinType = joinType;
    }
    
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize TableMapping to JSON", e);
        }
    }
    
    public static TableMapping fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, TableMapping.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize TableMapping from JSON", e);
        }
    }
    
    /**
     * Transformation rule for a column
     */
    public static class TransformationRule {
        private String type; // UPPERCASE, LOWERCASE, TRIM, REGEX_REPLACE, etc.
        private String pattern;
        private String replacement;
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getPattern() {
            return pattern;
        }
        
        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
        
        public String getReplacement() {
            return replacement;
        }
        
        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }
    }
    
    /**
     * Join type for translation
     */
    public enum JoinType {
        INNER,
        LEFT,
        RIGHT,
        FULL
    }
}