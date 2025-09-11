package gov.dhs.cbp.reference.events.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.dhs.cbp.reference.core.entity.Country;

import java.time.LocalDate;

/**
 * Event fired when a country is created, updated, or deleted
 */
public class CountryChangedEvent extends ReferenceDataEvent {

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("countryName")
    private String countryName;

    @JsonProperty("codeSystem")
    private String codeSystem;

    @JsonProperty("iso2Code")
    private String iso2Code;

    @JsonProperty("iso3Code")
    private String iso3Code;

    @JsonProperty("numericCode")
    private String numericCode;

    @JsonProperty("validFrom")
    private LocalDate validFrom;

    @JsonProperty("validTo")
    private LocalDate validTo;

    @JsonProperty("region")
    private String region;

    @JsonProperty("subRegion")
    private String subRegion;

    public CountryChangedEvent() {
        super();
        setAggregateType("Country");
    }

    /**
     * Create event from Country entity
     */
    public static CountryChangedEvent fromEntity(Country country, EventType eventType, String recordedBy) {
        CountryChangedEvent event = new CountryChangedEvent();
        event.setEventType(eventType);
        event.setAggregateId(country.getId().toString());
        event.setVersion(country.getVersion());
        event.setRecordedBy(recordedBy);
        
        event.setCountryCode(country.getCountryCode());
        event.setCountryName(country.getCountryName());
        event.setCodeSystem(country.getCodeSystem() != null ? country.getCodeSystem().getCode() : null);
        event.setIso2Code(country.getIso2Code());
        event.setIso3Code(country.getIso3Code());
        event.setNumericCode(country.getNumericCode());
        event.setValidFrom(country.getValidFrom());
        event.setValidTo(country.getValidTo());
        // Region fields not available in base Country entity
        event.setRegion(null);
        event.setSubRegion(null);
        
        return event;
    }

    // Getters and setters
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

    public String getCodeSystem() {
        return codeSystem;
    }

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
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

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSubRegion() {
        return subRegion;
    }

    public void setSubRegion(String subRegion) {
        this.subRegion = subRegion;
    }
}