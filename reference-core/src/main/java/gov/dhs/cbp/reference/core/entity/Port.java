package gov.dhs.cbp.reference.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Entity
@Table(name = "ports_v", schema = "reference_data",
       indexes = {
           @Index(name = "idx_port_code_system", columnList = "port_code,code_system_id,valid_from"),
           @Index(name = "idx_port_unlocode_system", columnList = "un_locode,code_system_id,valid_from"),
           @Index(name = "idx_port_cbp_code_system", columnList = "cbp_port_code,code_system_id,valid_from"),
           @Index(name = "idx_port_valid_dates", columnList = "valid_from,valid_to"),
           @Index(name = "idx_port_country", columnList = "country_code")
       })
public class Port extends Bitemporal {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_system_id", nullable = false)
    private CodeSystem codeSystem;
    
    @NotBlank
    @Size(max = 10)
    @Column(name = "port_code", nullable = false, length = 10)
    private String portCode;
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "port_name", nullable = false)
    private String portName;
    
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
    
    @Size(max = 50)
    @Column(name = "port_type", length = 50)
    private String portType;
    
    @Size(max = 5)
    @Column(name = "un_locode", length = 5)
    private String unLocode;
    
    @Size(max = 4)
    @Column(name = "cbp_port_code", length = 4)
    private String cbpPortCode;
    
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
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}