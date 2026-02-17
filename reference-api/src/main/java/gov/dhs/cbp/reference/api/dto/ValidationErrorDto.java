package gov.dhs.cbp.reference.api.dto;

public class ValidationErrorDto {
    private int row;
    private String column;
    private String field;
    private String value;
    private String message;
    private String severity;
    private String suggestion;

    public ValidationErrorDto() {
    }

    public ValidationErrorDto(int row, String column, String field, String value, String message, String severity, String suggestion) {
        this.row = row;
        this.column = column;
        this.field = field;
        this.value = value;
        this.message = message;
        this.severity = severity;
        this.suggestion = suggestion;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}
