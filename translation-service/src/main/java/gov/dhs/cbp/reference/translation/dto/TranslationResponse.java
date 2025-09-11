package gov.dhs.cbp.reference.translation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TranslationResponse {
    
    private String fromSystem;
    private String fromCode;
    private String toSystem;
    private String toCode;
    private BigDecimal confidence;
    private String mappingType;
    private LocalDate validFrom;
    private LocalDate validTo;
    private boolean deprecated;
    private String deprecationReason;
    private String ruleId;
    private List<String> alternativeCodes;
    
    public String getFromSystem() {
        return fromSystem;
    }
    
    public void setFromSystem(String fromSystem) {
        this.fromSystem = fromSystem;
    }
    
    public String getFromCode() {
        return fromCode;
    }
    
    public void setFromCode(String fromCode) {
        this.fromCode = fromCode;
    }
    
    public String getToSystem() {
        return toSystem;
    }
    
    public void setToSystem(String toSystem) {
        this.toSystem = toSystem;
    }
    
    public String getToCode() {
        return toCode;
    }
    
    public void setToCode(String toCode) {
        this.toCode = toCode;
    }
    
    public BigDecimal getConfidence() {
        return confidence;
    }
    
    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }
    
    public String getMappingType() {
        return mappingType;
    }
    
    public void setMappingType(String mappingType) {
        this.mappingType = mappingType;
    }
    
    public LocalDate getValidFrom() {
        return validFrom;
    }
    
    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }
    
    public LocalDate getValidTo() {
        return validTo;
    }
    
    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }
    
    public boolean isDeprecated() {
        return deprecated;
    }
    
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }
    
    public String getDeprecationReason() {
        return deprecationReason;
    }
    
    public void setDeprecationReason(String deprecationReason) {
        this.deprecationReason = deprecationReason;
    }
    
    public String getRuleId() {
        return ruleId;
    }
    
    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }
    
    public List<String> getAlternativeCodes() {
        return alternativeCodes;
    }
    
    public void setAlternativeCodes(List<String> alternativeCodes) {
        this.alternativeCodes = alternativeCodes;
    }
}