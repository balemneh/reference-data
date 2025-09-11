package gov.dhs.cbp.reference.catalog.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to register a consumer table with reference data mapping
 */
public class ConsumerTableRequest {
    
    @NotBlank
    @JsonProperty("consumer_id")
    private String consumerId;
    
    @NotBlank
    @JsonProperty("table_fqn")
    private String tableFqn;
    
    @NotNull
    @JsonProperty("reference_data_type")
    private ReferenceDataType referenceDataType;
    
    @JsonProperty("mapping")
    private TableMapping mapping;
    
    @JsonProperty("preferences")
    private TranslationPreferences preferences;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("contact_email")
    private String contactEmail;
    
    @JsonProperty("team_name")
    private String teamName;
    
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
        if (preferences == null) {
            preferences = new TranslationPreferences();
        }
        return preferences;
    }
    
    public void setPreferences(TranslationPreferences preferences) {
        this.preferences = preferences;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getContactEmail() {
        return contactEmail;
    }
    
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
    
    public String getTeamName() {
        return teamName;
    }
    
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}