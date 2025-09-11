package gov.dhs.cbp.reference.loader.common;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationResult {
    private final List<ValidationError> errors = new ArrayList<>();
    
    public void addError(ValidationError error) {
        errors.add(error);
    }
    
    public void addWarning(ValidationError warning) {
        errors.add(warning);
    }
    
    public boolean isValid() {
        return errors.stream()
            .noneMatch(e -> e.getSeverity() == ValidationError.Severity.ERROR);
    }
    
    public boolean hasWarnings() {
        return errors.stream()
            .anyMatch(e -> e.getSeverity() == ValidationError.Severity.WARNING);
    }
    
    public List<ValidationError> getErrors() {
        return errors;
    }
    
    public List<ValidationError> getErrorsBySeverity(ValidationError.Severity severity) {
        return errors.stream()
            .filter(e -> e.getSeverity() == severity)
            .collect(Collectors.toList());
    }
    
    public int getErrorCount() {
        return (int) errors.stream()
            .filter(e -> e.getSeverity() == ValidationError.Severity.ERROR)
            .count();
    }
    
    public int getWarningCount() {
        return (int) errors.stream()
            .filter(e -> e.getSeverity() == ValidationError.Severity.WARNING)
            .count();
    }
}