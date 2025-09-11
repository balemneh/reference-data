package gov.dhs.cbp.reference.catalog.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating a table mapping
 */
public class ValidationResult {
    
    private boolean valid;
    private List<ValidationError> errors = new ArrayList<>();
    private List<ValidationWarning> warnings = new ArrayList<>();
    private MappingScore score;
    
    public ValidationResult() {
        this.valid = true;
    }
    
    public boolean isValid() {
        return valid && errors.isEmpty();
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public List<ValidationError> getErrors() {
        return errors;
    }
    
    public void addError(String field, String message) {
        this.errors.add(new ValidationError(field, message));
        this.valid = false;
    }
    
    public List<ValidationWarning> getWarnings() {
        return warnings;
    }
    
    public void addWarning(String field, String message) {
        this.warnings.add(new ValidationWarning(field, message));
    }
    
    public MappingScore getScore() {
        return score;
    }
    
    public void setScore(MappingScore score) {
        this.score = score;
    }
    
    public static class ValidationError {
        private final String field;
        private final String message;
        
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }
        
        public String getField() {
            return field;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    public static class ValidationWarning {
        private final String field;
        private final String message;
        
        public ValidationWarning(String field, String message) {
            this.field = field;
            this.message = message;
        }
        
        public String getField() {
            return field;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    public static class MappingScore {
        private double confidence;
        private int matchedFields;
        private int totalFields;
        private boolean hasRequiredFields;
        
        public double getConfidence() {
            return confidence;
        }
        
        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
        
        public int getMatchedFields() {
            return matchedFields;
        }
        
        public void setMatchedFields(int matchedFields) {
            this.matchedFields = matchedFields;
        }
        
        public int getTotalFields() {
            return totalFields;
        }
        
        public void setTotalFields(int totalFields) {
            this.totalFields = totalFields;
        }
        
        public boolean isHasRequiredFields() {
            return hasRequiredFields;
        }
        
        public void setHasRequiredFields(boolean hasRequiredFields) {
            this.hasRequiredFields = hasRequiredFields;
        }
    }
}