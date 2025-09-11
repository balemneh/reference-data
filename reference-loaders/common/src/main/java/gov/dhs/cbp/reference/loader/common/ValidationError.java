package gov.dhs.cbp.reference.loader.common;

public class ValidationError {
    private final int recordIndex;
    private final String field;
    private final String message;
    private final Severity severity;
    
    public enum Severity {
        ERROR, WARNING, INFO
    }
    
    public ValidationError(int recordIndex, String field, String message, Severity severity) {
        this.recordIndex = recordIndex;
        this.field = field;
        this.message = message;
        this.severity = severity;
    }
    
    public int getRecordIndex() {
        return recordIndex;
    }
    
    public String getField() {
        return field;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] Record %d, Field '%s': %s", 
            severity, recordIndex, field, message);
    }
}