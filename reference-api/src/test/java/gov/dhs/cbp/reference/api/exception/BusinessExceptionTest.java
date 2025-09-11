package gov.dhs.cbp.reference.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    void constructor_WithAllParameters_SetsAllFields() {
        // Given
        String title = "Business Rule Violation";
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        String message = "Business rule failed";
        String type = "https://api.cbp.gov/errors/business-rule";

        // When
        BusinessException exception = new BusinessException(type, title, message, status);

        // Then
        assertThat(exception.getTitle()).isEqualTo(title);
        assertThat(exception.getStatus()).isEqualTo(status);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getType()).isEqualTo(type);
    }

    @Test
    void constructor_WithMinimalParameters_SetsRequiredFields() {
        // Given
        String message = "Invalid input data";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // When
        BusinessException exception = new BusinessException(message, status);

        // Then
        assertThat(exception.getTitle()).isEqualTo("Business Rule Violation");
        assertThat(exception.getStatus()).isEqualTo(status);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getType()).isEqualTo("https://api.cbp.gov/errors/business-error");
    }

    @Test
    void toString_ReturnsInformativeString() {
        // Given
        BusinessException exception = new BusinessException(
            "https://api.cbp.gov/errors/validation",
            "Validation Error", 
            "Field validation failed",
            HttpStatus.BAD_REQUEST
        );

        // When
        String result = exception.toString();

        // Then
        assertThat(result).contains("BusinessException");
        assertThat(result).contains("Field validation failed");
    }

    @Test
    void getters_ReturnCorrectValues() {
        // Given
        String title = "Custom Error";
        HttpStatus status = HttpStatus.FORBIDDEN;
        String message = "Access denied";
        String type = "https://api.cbp.gov/errors/access-denied";

        BusinessException exception = new BusinessException(type, title, message, status);

        // When & Then
        assertThat(exception.getTitle()).isEqualTo(title);
        assertThat(exception.getStatus()).isEqualTo(status);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getType()).isEqualTo(type);
    }
}