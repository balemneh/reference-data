package gov.dhs.cbp.reference.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Airport data transfer object")
public class AirportDto {
    
    @Schema(description = "Unique identifier for the airport", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "IATA airport code", example = "LAX", maxLength = 3)
    @Size(max = 3)
    private String iataCode;
    
    @Schema(description = "ICAO airport code", example = "KLAX", maxLength = 4)
    @Size(max = 4)
    private String icaoCode;
    
    @Schema(description = "Airport name", example = "Los Angeles International Airport", required = true)
    @NotBlank
    @Size(max = 255)
    private String airportName;
    
    @Schema(description = "City where the airport is located", example = "Los Angeles")
    @Size(max = 100)
    private String city;
    
    @Schema(description = "State or province where the airport is located", example = "California")
    @Size(max = 100)
    private String stateProvince;
    
    @Schema(description = "Country code (ISO 3166-1 alpha-3)", example = "USA", required = true)
    @NotBlank
    @Size(max = 3)
    private String countryCode;
    
    @Schema(description = "Airport latitude", example = "33.942536")
    private BigDecimal latitude;
    
    @Schema(description = "Airport longitude", example = "-118.408075")
    private BigDecimal longitude;
    
    @Schema(description = "Airport elevation in feet", example = "125")
    private Integer elevation;
    
    @Schema(description = "Type of airport", example = "International")
    @Size(max = 50)
    private String airportType;
    
    @Schema(description = "Airport timezone", example = "America/Los_Angeles")
    @Size(max = 50)
    private String timezone;
    
    @Schema(description = "Code system identifier", example = "IATA")
    private String codeSystem;
    
    @Schema(description = "Whether the airport is active", example = "true")
    private Boolean isActive;
    
    @Schema(description = "Date from which this record is valid")
    private LocalDate validFrom;
    
    @Schema(description = "Date until which this record is valid")
    private LocalDate validTo;
    
    @Schema(description = "Timestamp when this record was created")
    private LocalDateTime recordedAt;
    
    @Schema(description = "User who recorded this data")
    private String recordedBy;
    
    @Schema(description = "Version number for this record")
    private Long version;
    
    // Constructors
    public AirportDto() {}
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
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