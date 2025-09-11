package gov.dhs.cbp.reference.api.dto.search;

public class AutocompleteResult {
    
    private String code;
    private String name;
    private String matchType;
    private String entityType;
    
    public AutocompleteResult() {}
    
    public AutocompleteResult(String code, String name, String matchType) {
        this.code = code;
        this.name = name;
        this.matchType = matchType;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getMatchType() {
        return matchType;
    }
    
    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
}