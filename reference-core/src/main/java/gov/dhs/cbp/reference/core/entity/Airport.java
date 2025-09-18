package gov.dhs.cbp.reference.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Entity
@Table(name = "airports_v", schema = "reference_data",
       indexes = {
           @Index(name = "idx_airport_iata_code_system", columnList = "iata_code,code_system_id,valid_from"),
           @Index(name = "idx_airport_icao_code_system", columnList = "icao_code,code_system_id,valid_from"),
           @Index(name = "idx_airport_valid_dates", columnList = "valid_from,valid_to"),
           @Index(name = "idx_airport_country", columnList = "country_code")
       })
public class Airport extends Bitemporal {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_system_id", nullable = false)
    private CodeSystem codeSystem;
    
    @Size(max = 3)
    @Column(name = "iata_code", length = 3)
    private String iataCode;
    
    @Size(max = 4)
    @Column(name = "icao_code", length = 4)
    private String icaoCode;
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "airport_name", nullable = false)
    private String airportName;
    
    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;
    
    @Size(max = 100)
    @Column(name = "state_province", length = 100)
    private String stateProvince;
    
    @NotBlank
    @Size(max = 3)
    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode;
    
    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;
    
    @Column(name = "elevation")
    private Integer elevation;
    
    @Size(max = 50)
    @Column(name = "airport_type", length = 50)
    private String airportType;
    
    @Size(max = 50)
    @Column(name = "timezone", length = 50)
    private String timezone;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    public CodeSystem getCodeSystem() {
        return codeSystem;
    }
    
    public void setCodeSystem(CodeSystem codeSystem) {
        this.codeSystem = codeSystem;
    }
    
    public String getIataCode() {
        return iataCode;
    }
    
    public void setIataCode(String iataCode) {
        this.iataCode = iataCode;
    }
    
    public String getIcaoCode() {
        return icaoCode;
    }
    
    public void setIcaoCode(String icaoCode) {
        this.icaoCode = icaoCode;
    }
    
    public String getAirportName() {
        return airportName;
    }
    
    public void setAirportName(String airportName) {
        this.airportName = airportName;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getStateProvince() {
        return stateProvince;
    }
    
    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    
    public BigDecimal getLatitude() {
        return latitude;
    }
    
    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }
    
    public BigDecimal getLongitude() {
        return longitude;
    }
    
    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }
    
    public Integer getElevation() {
        return elevation;
    }
    
    public void setElevation(Integer elevation) {
        this.elevation = elevation;
    }
    
    public String getAirportType() {
        return airportType;
    }
    
    public void setAirportType(String airportType) {
        this.airportType = airportType;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}