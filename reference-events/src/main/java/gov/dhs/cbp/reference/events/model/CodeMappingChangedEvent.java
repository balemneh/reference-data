package gov.dhs.cbp.reference.events.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.dhs.cbp.reference.core.entity.CodeMapping;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Event fired when a code mapping is created, updated, or deleted
 */
public class CodeMappingChangedEvent extends ReferenceDataEvent {

    @JsonProperty("fromSystem")
    private String fromSystem;

    @JsonProperty("fromCode")
    private String fromCode;

    @JsonProperty("toSystem")
    private String toSystem;

    @JsonProperty("toCode")
    private String toCode;

    @JsonProperty("ruleId")
    private String ruleId;

    @JsonProperty("confidence")
    private BigDecimal confidence;

    @JsonProperty("mappingType")
    private String mappingType;

    @JsonProperty("isDeprecated")
    private Boolean isDeprecated;

    @JsonProperty("deprecationReason")
    private String deprecationReason;

    @JsonProperty("validFrom")
    private LocalDate validFrom;

    @JsonProperty("validTo")
    private LocalDate validTo;

    public CodeMappingChangedEvent() {
        super();
        setAggregateType("CodeMapping");
    }

    /**
     * Create event from CodeMapping entity
     */
    public static CodeMappingChangedEvent fromEntity(CodeMapping mapping, EventType eventType, String recordedBy) {
        CodeMappingChangedEvent event = new CodeMappingChangedEvent();
        event.setEventType(eventType);
        event.setAggregateId(mapping.getId().toString());
        event.setVersion(mapping.getVersion());
        event.setRecordedBy(recordedBy);
        
        event.setFromSystem(mapping.getFromSystem() != null ? mapping.getFromSystem().getCode() : null);
        event.setFromCode(mapping.getFromCode());
        event.setToSystem(mapping.getToSystem() != null ? mapping.getToSystem().getCode() : null);
        event.setToCode(mapping.getToCode());
        event.setRuleId(mapping.getRuleId());
        event.setConfidence(mapping.getConfidence());
        event.setMappingType(mapping.getMappingType());
        event.setIsDeprecated(mapping.getIsDeprecated());
        event.setDeprecationReason(mapping.getDeprecationReason());
        event.setValidFrom(mapping.getValidFrom());
        event.setValidTo(mapping.getValidTo());
        
        return event;
    }

    // Getters and setters
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

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
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

    public Boolean getIsDeprecated() {
        return isDeprecated;
    }

    public void setIsDeprecated(Boolean isDeprecated) {
        this.isDeprecated = isDeprecated;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }

    public void setDeprecationReason(String deprecationReason) {
        this.deprecationReason = deprecationReason;
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
}