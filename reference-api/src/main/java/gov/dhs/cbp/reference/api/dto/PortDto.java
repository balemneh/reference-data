package gov.dhs.cbp.reference.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Port data transfer object")
public class PortDto {
    
    @Schema(description = "Unique identifier for the port", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "Port code", example = "USNYC", required = true)
    @NotBlank
    @Size(max = 10)
    private String portCode;
    
    @Schema(description = "Port name", example = "Port of New York and New Jersey", required = true)
    @NotBlank
    @Size(max = 255)
    private String portName;
    
    @Schema(description = "City where the port is located", example = "New York")
    @Size(max = 100)
    private String city;
    
    @Schema(description = "State or province where the port is located", example = "New York")
    @Size(max = 100)
    private String stateProvince;
    
    @Schema(description = "Country code (ISO 3166-1 alpha-3)", example = "USA", required = true)
    @NotBlank
    @Size(max = 3)
    private String countryCode;
    
    @Schema(description = "Port latitude", example = "40.6892")
    private BigDecimal latitude;
    
    @Schema(description = "Port longitude", example = "-74.0445")
    private BigDecimal longitude;
    
    @Schema(description = "Type of port", example = "Container")
    @Size(max = 50)
    private String portType;
    
    @Schema(description = "UN/LOCODE (United Nations Location Code)", example = "USNYC")
    @Size(max = 5)
    private String unLocode;
    
    @Schema(description = "CBP port code", example = "4710")
    @Size(max = 4)
    private String cbpPortCode;
    
    @Schema(description = "Port timezone", example = "America/New_York")
    @Size(max = 50)
    private String timezone;
    
    @Schema(description = "Code system identifier", example = "UN-LOCODE")
    private String codeSystem;
    
    @Schema(description = "Whether the port is active", example = "true")
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
    public PortDto() {}
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getPortCode() {
        return portCode;
    }
    
    public void setPortCode(String portCode) {
        this.portCode = portCode;
    }
    
    public String getPortName() {
        return portName;
    }
    
    public void setPortName(String portName) {
        this.portName = portName;
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
    
    public String getPortType() {
        return portType;
    }
    
    public void setPortType(String portType) {
        this.portType = portType;
    }
    
    public String getUnLocode() {
        return unLocode;
    }
    
    public void setUnLocode(String unLocode) {
        this.unLocode = unLocode;
    }
    
    public String getCbpPortCode() {
        return cbpPortCode;
    }
    
    public void setCbpPortCode(String cbpPortCode) {
        this.cbpPortCode = cbpPortCode;
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