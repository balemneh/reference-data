package gov.dhs.cbp.reference.catalog.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Represents a consumer's registration for a specific table
 * mapped to reference data.
 */
public class ConsumerRegistration {
    
    @JsonProperty("consumer_id")
    private String consumerId;
    
    @JsonProperty("table_fqn")
    private String tableFqn;
    
    @JsonProperty("reference_data_type")
    private ReferenceDataType referenceDataType;
    
    @JsonProperty("mapping")
    private TableMapping mapping;
    
    @JsonProperty("preferences")
    private TranslationPreferences preferences;
    
    @JsonProperty("registered_at")
    private LocalDateTime registeredAt;
    
    @JsonProperty("last_updated")
    private LocalDateTime lastUpdated;
    
    @JsonProperty("deactivated_at")
    private LocalDateTime deactivatedAt;
    
    @JsonProperty("active")
    private boolean active;
    
    @JsonProperty("version")
    private String version;
    
    // Getters and setters
    public String getConsumerId() {
        return consumerId;
    }
    
    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }
    
    public String getTableFqn() {
        return tableFqn;
    }
    
    public void setTableFqn(String tableFqn) {
        this.tableFqn = tableFqn;
    }
    
    public ReferenceDataType getReferenceDataType() {
        return referenceDataType;
    }
    
    public void setReferenceDataType(ReferenceDataType referenceDataType) {
        this.referenceDataType = referenceDataType;
    }
    
    public TableMapping getMapping() {
        return mapping;
    }
    
    public void setMapping(TableMapping mapping) {
        this.mapping = mapping;
    }
    
    public TranslationPreferences getPreferences() {
        return preferences;
    }
    
    public void setPreferences(TranslationPreferences preferences) {
        this.preferences = preferences;
    }
    
    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }
    
    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public LocalDateTime getDeactivatedAt() {
        return deactivatedAt;
    }
    
    public void setDeactivatedAt(LocalDateTime deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
}