package gov.dhs.cbp.reference.api.exception;

import gov.dhs.cbp.reference.api.dto.ProblemDetail;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        
        ProblemDetail problem = ProblemDetail.notFound(ex.getMessage());
        problem.setInstance(request.getRequestURI());
        problem.setTraceId(getOrGenerateTraceId());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ProblemDetail problem = ProblemDetail.badRequest("Validation failed");
        problem.setErrors(errors);
        problem.setInstance(request.getRequestURI());
        problem.setTraceId(getOrGenerateTraceId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        });
        
        ProblemDetail problem = ProblemDetail.badRequest("Constraint violation");
        problem.setErrors(errors);
        problem.setInstance(request.getRequestURI());
        problem.setTraceId(getOrGenerateTraceId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        
        String message = "Data integrity violation";
        if (ex.getMessage() != null && ex.getMessage().contains("duplicate key")) {
            message = "Duplicate key violation";
        }
        
        ProblemDetail problem = ProblemDetail.conflict(message);
        problem.setInstance(request.getRequestURI());
        problem.setTraceId(getOrGenerateTraceId());
        
        logger.error("Data integrity violation: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String message = String.format("Required parameter '%s' is missing", 
                ex.getParameterName());
        
        ProblemDetail problem = ProblemDetail.badRequest(message);
        problem.setInstance(request.getRequestURI());
        problem.setTraceId(getOrGenerateTraceId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String message = String.format("Invalid value '%s' for parameter '%s'", 
                ex.getValue(), ex.getName());
        
        ProblemDetail problem = ProblemDetail.badRequest(message);
        problem.setInstance(request.getRequestURI());
        problem.setTraceId(getOrGenerateTraceId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        ProblemDetail problem = ProblemDetail.notFound(ex.getMessage());
        problem.setType("https://api.cbp.gov/errors/resource-not-found");
        problem.setInstance(request.getRequestURI());
        problem.setTraceId(getOrGenerateTraceId());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        ProblemDetail problem = new ProblemDetail(
                ex.getTitle(), 
                ex.getStatus().value(), 
                ex.getMessage()
        );
        problem.setType(ex.getType());
        problem.setInstance(request.getRequestURI());
        problem.setTraceId(getOrGenerateTraceId());
        
        return ResponseEntity.status(ex.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        ProblemDetail problem = ProblemDetail.badRequest(ex.getMessage());
        problem.setInstance(request.getRequestURI());
        problem.setTraceId(getOrGenerateTraceId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String traceId = getOrGenerateTraceId();
        logger.error("Unhandled exception [traceId={}]", traceId, ex);
        
        ProblemDetail problem = ProblemDetail.internalServerError(
                "An unexpected error occurred. Please contact support with trace ID: " + traceId
        );
        problem.setInstance(request.getRequestURI());
        problem.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
    
    private String getOrGenerateTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
        }
        return traceId;
    }
}