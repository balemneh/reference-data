package gov.dhs.cbp.reference.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "code_mapping", schema = "reference_data",
       indexes = {
           @Index(name = "idx_mapping_from", columnList = "from_system_id,from_code"),
           @Index(name = "idx_mapping_to", columnList = "to_system_id,to_code"),
           @Index(name = "idx_mapping_valid", columnList = "valid_from,valid_to")
       })
public class CodeMapping extends Bitemporal {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_system_id", nullable = false)
    private CodeSystem fromSystem;
    
    @NotBlank
    @Column(name = "from_code", nullable = false, length = 50)
    private String fromCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_system_id", nullable = false)
    private CodeSystem toSystem;
    
    @NotBlank
    @Column(name = "to_code", nullable = false, length = 50)
    private String toCode;
    
    @Column(name = "rule_id", length = 100)
    private String ruleId;
    
    @NotNull
    @Column(name = "confidence", precision = 5, scale = 2)
    private BigDecimal confidence = BigDecimal.valueOf(100);
    
    @Column(name = "mapping_type", length = 50)
    private String mappingType;
    
    @Column(name = "is_deprecated", nullable = false)
    private Boolean isDeprecated = false;
    
    @Column(name = "deprecation_reason", columnDefinition = "TEXT")
    private String deprecationReason;
    
    public CodeSystem getFromSystem() {
        return fromSystem;
    }
    
    public void setFromSystem(CodeSystem fromSystem) {
        this.fromSystem = fromSystem;
    }
    
    public String getFromCode() {
        return fromCode;
    }
    
    public void setFromCode(String fromCode) {
        this.fromCode = fromCode;
    }
    
    public CodeSystem getToSystem() {
        return toSystem;
    }
    
    public void setToSystem(CodeSystem toSystem) {
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
}