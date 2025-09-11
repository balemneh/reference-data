package gov.dhs.cbp.reference.api.exception;

import gov.dhs.cbp.reference.api.dto.ProblemDetail;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        given(request.getRequestURI()).willReturn("/v1/test");
        MDC.clear();
    }

    @Test
    void handleEntityNotFound_ReturnsNotFoundResponse() {
        // Given
        EntityNotFoundException ex = new EntityNotFoundException("Entity not found");

        // When
        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleEntityNotFound(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
        assertThat(response.getBody().getDetail()).isEqualTo("Entity not found");
        assertThat(response.getBody().getInstance()).isEqualTo("/v1/test");
        assertThat(response.getBody().getTraceId()).isNotNull();
    }

    @Test
    void handleValidationExceptions_ReturnsBadRequestResponse() {
        // Given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("testObject", "testField", "Test validation message");
        given(ex.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getAllErrors()).willReturn(java.util.Arrays.asList(fieldError));

        // When
        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleValidationExceptions(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
        assertThat(response.getBody().getDetail()).isEqualTo("Validation failed");
        assertThat(response.getBody().getErrors()).containsKey("testField");
        assertThat(response.getBody().getErrors().get("testField")).isEqualTo("Test validation message");
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleConstraintViolation_ReturnsBadRequestResponse() {
        // Given
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        given(violation.getPropertyPath()).willReturn(path);
        given(path.toString()).willReturn("testProperty");
        given(violation.getMessage()).willReturn("Constraint violation message");

        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violation);

        ConstraintViolationException ex = new ConstraintViolationException("Constraint violation", violations);

        // When
        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleConstraintViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
        assertThat(response.getBody().getDetail()).isEqualTo("Constraint violation");
        assertThat(response.getBody().getErrors()).containsKey("testProperty");
        assertThat(response.getBody().getErrors().get("testProperty")).isEqualTo("Constraint violation message");
    }

    @Test
    void handleDataIntegrityViolation_ReturnsConflictResponse() {
        // Given
        DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicate key violation");

        // When
        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleDataIntegrityViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Conflict");
        assertThat(response.getBody().getDetail()).isEqualTo("Duplicate key violation");
    }

    @Test
    void handleDataIntegrityViolation_WithGenericMessage_ReturnsConflictResponse() {
        // Given
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Some other error");

        // When
        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleDataIntegrityViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Data integrity violation");
    }

    @Test
    void handleTypeMismatch_ReturnsBadRequestResponse() {
        // Given
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        given(ex.getValue()).willReturn("invalid-uuid");
        given(ex.getName()).willReturn("id");

        // When
        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleTypeMismatch(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).contains("Invalid value 'invalid-uuid' for parameter 'id'");
    }

    @Test
    void handleResourceNotFound_ReturnsNotFoundResponse() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");

        // When
        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleResourceNotFound(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo("https://api.cbp.gov/errors/resource-not-found");
        assertThat(response.getBody().getDetail()).isEqualTo("Resource not found");
    }

    @Test
    void handleBusinessException_ReturnsCustomStatusResponse() {
        // Given
        BusinessException ex = new BusinessException(
                "https://api.cbp.gov/errors/business-rule",
                "Business Rule Violation",
                "Business rule failed",
                HttpStatus.UNPROCESSABLE_ENTITY
        );

        // When
        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleBusinessException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Business Rule Violation");
        assertThat(response.getBody().getDetail()).isEqualTo("Business rule failed");
        assertThat(response.getBody().getType()).isEqualTo("https://api.cbp.gov/errors/business-rule");
    }

    @Test
    void handleGenericException_ReturnsInternalServerErrorResponse() {
        // Given
        Exception ex = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleGenericException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getDetail()).contains("An unexpected error occurred");
        assertThat(response.getBody().getDetail()).contains("trace ID:");
    }

    @Test
    void getOrGenerateTraceId_WithExistingTraceId_ReturnsExistingTraceId() {
        // Given
        String existingTraceId = UUID.randomUUID().toString();
        MDC.put("traceId", existingTraceId);

        // When
        Exception ex = new RuntimeException("Test");
        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleGenericException(ex, request);

        // Then
        assertThat(response.getBody().getTraceId()).isEqualTo(existingTraceId);
    }

    @Test
    void getOrGenerateTraceId_WithNoExistingTraceId_GeneratesNewTraceId() {
        // Given
        MDC.clear();

        // When
        Exception ex = new RuntimeException("Test");
        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleGenericException(ex, request);

        // Then
        assertThat(response.getBody().getTraceId()).isNotNull();
        assertThat(UUID.fromString(response.getBody().getTraceId())).isNotNull();
    }
}