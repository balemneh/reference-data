package gov.dhs.cbp.reference.api.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void constructor_WithMessage_SetsMessage() {
        // Given
        String message = "Resource not found";

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void constructor_WithNullMessage_HandlesGracefully() {
        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(null);

        // Then
        assertThat(exception.getMessage()).isNull();
    }

    @Test
    void constructor_WithEmptyMessage_HandlesGracefully() {
        // Given
        String emptyMessage = "";

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(emptyMessage);

        // Then
        assertThat(exception.getMessage()).isEqualTo(emptyMessage);
    }

    @Test
    void exception_IsRuntimeException() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Test message");

        // When & Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isInstanceOf(Exception.class);
    }

    @Test
    void toString_ContainsClassName() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Test resource not found");

        // When
        String result = exception.toString();

        // Then
        assertThat(result).contains("ResourceNotFoundException");
        assertThat(result).contains("Test resource not found");
    }

    @Test
    void getMessage_ReturnsCorrectMessage() {
        // Given
        String message = "The requested country was not found";
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        // When
        String result = exception.getMessage();

        // Then
        assertThat(result).isEqualTo(message);
    }
}