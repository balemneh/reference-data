package gov.dhs.cbp.reference.catalog.model;

/**
 * Enum representing the types of reference data available
 */
public enum ReferenceDataType {
    COUNTRY("country", "Country reference data");
    
    private final String code;
    private final String description;
    
    ReferenceDataType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static ReferenceDataType fromCode(String code) {
        for (ReferenceDataType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown reference data type: " + code);
    }
}