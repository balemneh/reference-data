package gov.dhs.cbp.reference.loader.common;

import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service for validating source data before staging
 */
@Service
public class ValidationService<T> {
    
    private final Validator validator;
    private final List<ValidationRule<T>> customRules;
    
    public ValidationService(Validator validator) {
        this.validator = validator;
        this.customRules = new ArrayList<>();
    }
    
    public void addRule(ValidationRule<T> rule) {
        customRules.add(rule);
    }
    
    public ValidationResult validate(List<T> data) {
        ValidationResult result = new ValidationResult();
        
        for (int i = 0; i < data.size(); i++) {
            T record = data.get(i);
            
            // Bean validation
            @SuppressWarnings("unchecked")
            Set<ConstraintViolation<T>> violations = (Set<ConstraintViolation<T>>) validator.validate(record);
            for (ConstraintViolation<T> violation : violations) {
                result.addError(new ValidationError(
                    i,
                    violation.getPropertyPath().toString(),
                    violation.getMessage(),
                    ValidationError.Severity.ERROR
                ));
            }
            
            // Custom rules
            for (ValidationRule<T> rule : customRules) {
                ValidationRule.Result ruleResult = rule.validate(record);
                if (!ruleResult.isValid()) {
                    result.addError(new ValidationError(
                        i,
                        rule.getName(),
                        ruleResult.getMessage(),
                        ruleResult.getSeverity()
                    ));
                }
            }
        }
        
        return result;
    }
    
    /**
     * Interface for custom validation rules
     */
    public interface ValidationRule<T> {
        String getName();
        Result validate(T record);
        
        class Result {
            private final boolean valid;
            private final String message;
            private final ValidationError.Severity severity;
            
            public Result(boolean valid, String message, ValidationError.Severity severity) {
                this.valid = valid;
                this.message = message;
                this.severity = severity;
            }
            
            public static Result valid() {
                return new Result(true, null, null);
            }
            
            public static Result invalid(String message) {
                return new Result(false, message, ValidationError.Severity.ERROR);
            }
            
            public static Result warning(String message) {
                return new Result(false, message, ValidationError.Severity.WARNING);
            }
            
            public boolean isValid() {
                return valid;
            }
            
            public String getMessage() {
                return message;
            }
            
            public ValidationError.Severity getSeverity() {
                return severity;
            }
        }
    }
}