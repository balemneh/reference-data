package gov.dhs.cbp.reference.loader.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {
    
    @Mock
    private Validator validator;
    
    private ValidationService<TestData> validationService;
    
    @BeforeEach
    void setUp() {
        validationService = new ValidationService<>(validator);
    }
    
    @Test
    void testValidateWithNoErrors() {
        TestData data = new TestData("ABC", "Test");
        List<TestData> dataList = Arrays.asList(data);
        
        when(validator.validate(any())).thenReturn(new HashSet<>());
        
        ValidationResult result = validationService.validate(dataList);
        
        assertTrue(result.isValid());
        assertEquals(0, result.getErrorCount());
        assertFalse(result.hasWarnings());
    }
    
    @Test
    void testValidateWithConstraintViolations() {
        TestData data = new TestData("", "Test");
        List<TestData> dataList = Arrays.asList(data);
        
        @SuppressWarnings("unchecked")
        ConstraintViolation<TestData> violation = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("code");
        when(violation.getMessage()).thenReturn("Code is required");
        
        @SuppressWarnings({"unchecked", "rawtypes"})
        Set<ConstraintViolation<Object>> violations = new HashSet<>();
        violations.add((ConstraintViolation) violation);
        
        when(validator.validate(any())).thenReturn(violations);
        
        ValidationResult result = validationService.validate(dataList);
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrorCount());
        assertEquals("Code is required", result.getErrors().get(0).getMessage());
    }
    
    @Test
    void testCustomValidationRule() {
        TestData data1 = new TestData("ABC", "Test");
        TestData data2 = new TestData("XYZ", "Invalid");
        List<TestData> dataList = Arrays.asList(data1, data2);
        
        when(validator.validate(any())).thenReturn(new HashSet<>());
        
        // Add custom rule that fails for "XYZ"
        validationService.addRule(new ValidationService.ValidationRule<TestData>() {
            @Override
            public String getName() {
                return "CustomCodeCheck";
            }
            
            @Override
            public Result validate(TestData record) {
                if ("XYZ".equals(record.getCode())) {
                    return Result.invalid("XYZ is not allowed");
                }
                return Result.valid();
            }
        });
        
        ValidationResult result = validationService.validate(dataList);
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrorCount());
        assertEquals("XYZ is not allowed", result.getErrors().get(0).getMessage());
    }
    
    @Test
    void testWarningValidation() {
        TestData data = new TestData("WARN", "Test");
        List<TestData> dataList = Arrays.asList(data);
        
        when(validator.validate(any())).thenReturn(new HashSet<>());
        
        validationService.addRule(new ValidationService.ValidationRule<TestData>() {
            @Override
            public String getName() {
                return "WarningCheck";
            }
            
            @Override
            public Result validate(TestData record) {
                if ("WARN".equals(record.getCode())) {
                    return Result.warning("Code WARN might cause issues");
                }
                return Result.valid();
            }
        });
        
        ValidationResult result = validationService.validate(dataList);
        
        assertTrue(result.isValid()); // Warnings don't make it invalid
        assertTrue(result.hasWarnings());
        assertEquals(0, result.getErrorCount());
        assertEquals(1, result.getWarningCount());
    }
    
    @Test
    void testMultipleRecordsValidation() {
        List<TestData> dataList = Arrays.asList(
            new TestData("ABC", "Test1"),
            new TestData("", "Test2"), // Invalid
            new TestData("XYZ", "Test3")
        );
        
        // Mock constraint violation for empty code
        @SuppressWarnings("unchecked")
        ConstraintViolation<TestData> violation = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("code");
        when(violation.getMessage()).thenReturn("Code is required");
        
        @SuppressWarnings({"unchecked", "rawtypes"})
        Set<ConstraintViolation<Object>> violations = new HashSet<>();
        violations.add((ConstraintViolation) violation);
        
        when(validator.validate(argThat(data -> data != null && "".equals(((TestData)data).getCode()))))
            .thenReturn(violations);
        when(validator.validate(argThat(data -> data != null && !"".equals(((TestData)data).getCode()))))
            .thenReturn(new HashSet<>());
        
        ValidationResult result = validationService.validate(dataList);
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrorCount());
        assertEquals(1, result.getErrors().get(0).getRecordIndex()); // Second record
    }
    
    @Test
    void testValidationErrorSeverity() {
        TestData data = new TestData("ERROR", "Test");
        List<TestData> dataList = Arrays.asList(data);
        
        when(validator.validate(any())).thenReturn(new HashSet<>());
        
        validationService.addRule(new ValidationService.ValidationRule<TestData>() {
            @Override
            public String getName() {
                return "SeverityCheck";
            }
            
            @Override
            public Result validate(TestData record) {
                return Result.invalid("Critical error");
            }
        });
        
        ValidationResult result = validationService.validate(dataList);
        
        List<ValidationError> errors = result.getErrorsBySeverity(ValidationError.Severity.ERROR);
        assertEquals(1, errors.size());
        assertEquals(ValidationError.Severity.ERROR, errors.get(0).getSeverity());
    }
    
    // Test data class
    static class TestData {
        @NotBlank
        private String code;
        
        @Size(max = 100)
        private String name;
        
        public TestData(String code, String name) {
            this.code = code;
            this.name = name;
        }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}