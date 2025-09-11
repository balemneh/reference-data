package gov.dhs.cbp.reference.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "countries_v", schema = "reference_data",
       indexes = {
           @Index(name = "idx_country_code_system", columnList = "country_code,code_system_id,valid_from"),
           @Index(name = "idx_country_valid_dates", columnList = "valid_from,valid_to")
       })
public class Country extends Bitemporal {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_system_id", nullable = false)
    private CodeSystem codeSystem;
    
    @NotBlank
    @Size(max = 10)
    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode;
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "country_name", nullable = false)
    private String countryName;
    
    @Size(max = 2)
    @Column(name = "iso2_code", length = 2)
    private String iso2Code;
    
    @Size(max = 3)
    @Column(name = "iso3_code", length = 3)
    private String iso3Code;
    
    @Size(max = 3)
    @Column(name = "numeric_code", length = 3)
    private String numericCode;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    // Alias methods for compatibility
    public String getAlpha2Code() {
        return iso2Code;
    }
    
    public void setAlpha2Code(String alpha2Code) {
        this.iso2Code = alpha2Code;
    }
    
    public String getAlpha3Code() {
        return iso3Code;
    }
    
    public void setAlpha3Code(String alpha3Code) {
        this.iso3Code = alpha3Code;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public CodeSystem getCodeSystem() {
        return codeSystem;
    }
    
    public void setCodeSystem(CodeSystem codeSystem) {
        this.codeSystem = codeSystem;
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
}