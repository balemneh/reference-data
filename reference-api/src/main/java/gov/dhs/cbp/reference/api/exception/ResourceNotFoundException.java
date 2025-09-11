package gov.dhs.cbp.reference.api.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    private final String resourceType;
    private final String identifier;
    
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s not found with identifier: %s", resourceType, identifier));
        this.resourceType = resourceType;
        this.identifier = identifier;
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.identifier = null;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public String getIdentifier() {
        return identifier;
    }
}