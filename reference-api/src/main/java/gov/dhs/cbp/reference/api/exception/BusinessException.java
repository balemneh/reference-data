package gov.dhs.cbp.reference.api.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    
    private final String type;
    private final String title;
    private final HttpStatus status;
    
    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.type = "https://api.cbp.gov/errors/business-error";
        this.title = "Business Rule Violation";
        this.status = status;
    }
    
    public BusinessException(String type, String title, String message, HttpStatus status) {
        super(message);
        this.type = type;
        this.title = title;
        this.status = status;
    }
    
    public String getType() {
        return type;
    }
    
    public String getTitle() {
        return title;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
}