package gov.dhs.cbp.reference.loaders.iso.entity;

import gov.dhs.cbp.reference.loader.common.StagingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "iso_countries_staging", schema = "staging",
    indexes = {
        @Index(name = "idx_iso_staging_alpha2", columnList = "alpha2_code"),
        @Index(name = "idx_iso_staging_alpha3", columnList = "alpha3_code"),
        @Index(name = "idx_iso_staging_exec", columnList = "load_execution_id")
    })
public class IsoCountryStaging extends StagingEntity {
    
    @NotBlank(message = "Country name is required")
    @Size(max = 255)
    @Column(name = "country_name", nullable = false)
    private String countryName;
    
    @NotBlank(message = "Alpha-2 code is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Alpha-2 code must be 2 uppercase letters")
    @Column(name = "alpha2_code", nullable = false, length = 2)
    private String alpha2Code;
    
    @NotBlank(message = "Alpha-3 code is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Alpha-3 code must be 3 uppercase letters")
    @Column(name = "alpha3_code", nullable = false, length = 3)
    private String alpha3Code;
    
    @Pattern(regexp = "^[0-9]{3}$", message = "Numeric code must be 3 digits")
    @Column(name = "numeric_code", length = 3)
    private String numericCode;
    
    @Column(name = "official_name")
    private String officialName;
    
    @Column(name = "common_name")
    private String commonName;
    
    @Column(name = "capital")
    private String capital;
    
    @Column(name = "region")
    private String region;
    
    @Column(name = "subregion")
    private String subregion;
    
    @Column(name = "continent")
    private String continent;
    
    @Column(name = "is_independent")
    private Boolean isIndependent;
    
    @Column(name = "is_un_member")
    private Boolean isUnMember;
    
    @Column(name = "currency_code", length = 3)
    private String currencyCode;
    
    @Column(name = "currency_name")
    private String currencyName;
    
    @Column(name = "phone_code")
    private String phoneCode;
    
    @Column(name = "tld")
    private String tld; // Top-level domain
    
    @Column(name = "languages")
    private String languages; // Comma-separated list
    
    @Column(name = "population")
    private Long population;
    
    @Column(name = "area_sq_km")
    private Double areaSqKm;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "source_file")
    private String sourceFile;
    
    @Column(name = "source_date")
    private String sourceDate;
    
    // Getters and setters
    public String getCountryName() {
        return countryName;
    }
    
    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }
    
    public String getAlpha2Code() {
        return alpha2Code;
    }
    
    public void setAlpha2Code(String alpha2Code) {
        this.alpha2Code = alpha2Code;
    }
    
    public String getAlpha3Code() {
        return alpha3Code;
    }
    
    public void setAlpha3Code(String alpha3Code) {
        this.alpha3Code = alpha3Code;
    }
    
    public String getNumericCode() {
        return numericCode;
    }
    
    public void setNumericCode(String numericCode) {
        this.numericCode = numericCode;
    }
    
    public String getOfficialName() {
        return officialName;
    }
    
    public void setOfficialName(String officialName) {
        this.officialName = officialName;
    }
    
    public String getCommonName() {
        return commonName;
    }
    
    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }
    
    public String getCapital() {
        return capital;
    }
    
    public void setCapital(String capital) {
        this.capital = capital;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getSubregion() {
        return subregion;
    }
    
    public void setSubregion(String subregion) {
        this.subregion = subregion;
    }
    
    public String getContinent() {
        return continent;
    }
    
    public void setContinent(String continent) {
        this.continent = continent;
    }
    
    public Boolean getIsIndependent() {
        return isIndependent;
    }
    
    public void setIsIndependent(Boolean isIndependent) {
        this.isIndependent = isIndependent;
    }
    
    public Boolean getIsUnMember() {
        return isUnMember;
    }
    
    public void setIsUnMember(Boolean isUnMember) {
        this.isUnMember = isUnMember;
    }
    
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
    
    public String getCurrencyName() {
        return currencyName;
    }
    
    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }
    
    public String getPhoneCode() {
        return phoneCode;
    }
    
    public void setPhoneCode(String phoneCode) {
        this.phoneCode = phoneCode;
    }
    
    public String getTld() {
        return tld;
    }
    
    public void setTld(String tld) {
        this.tld = tld;
    }
    
    public String getLanguages() {
        return languages;
    }
    
    public void setLanguages(String languages) {
        this.languages = languages;
    }
    
    public Long getPopulation() {
        return population;
    }
    
    public void setPopulation(Long population) {
        this.population = population;
    }
    
    public Double getAreaSqKm() {
        return areaSqKm;
    }
    
    public void setAreaSqKm(Double areaSqKm) {
        this.areaSqKm = areaSqKm;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public String getSourceFile() {
        return sourceFile;
    }
    
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }
    
    public String getSourceDate() {
        return sourceDate;
    }
    
    public void setSourceDate(String sourceDate) {
        this.sourceDate = sourceDate;
    }
}