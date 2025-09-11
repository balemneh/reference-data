package gov.dhs.cbp.reference.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class CountryDto {
    
    private UUID id;
    private String countryCode;
    private String countryName;
    private String iso2Code;
    private String iso3Code;
    private String numericCode;
    private String codeSystem;
    private Boolean isActive;
    private LocalDate validFrom;
    private LocalDate validTo;
    private LocalDateTime recordedAt;
    private String recordedBy;
    private Long version;
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    
    public String getCountryName() {
        return countryName;
    }
    
    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }
    
    public String getIso2Code() {
        return iso2Code;
    }
    
    public void setIso2Code(String iso2Code) {
        this.iso2Code = iso2Code;
    }
    
    public String getIso3Code() {
        return iso3Code;
    }
    
    public void setIso3Code(String iso3Code) {
        this.iso3Code = iso3Code;
    }
    
    public String getNumericCode() {
        return numericCode;
    }
    
    public void setNumericCode(String numericCode) {
        this.numericCode = numericCode;
    }
    
    public String getCodeSystem() {
        return codeSystem;
    }
    
    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
    
    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
    
    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
    
    public String getRecordedBy() {
        return recordedBy;
    }
    
    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}