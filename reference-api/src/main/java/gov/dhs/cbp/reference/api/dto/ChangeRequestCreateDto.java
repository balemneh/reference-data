package gov.dhs.cbp.reference.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ChangeRequestCreateDto {

    @NotNull(message = "Country data is required")
    private CountryDto countryData;

    @NotBlank(message = "Reason is required")
    private String reason;

    // Default constructor
    public ChangeRequestCreateDto() {
    }

    // Constructor with parameters
    public ChangeRequestCreateDto(CountryDto countryData, String reason) {
        this.countryData = countryData;
        this.reason = reason;
    }

    // Getters and setters
    public CountryDto getCountryData() {
        return countryData;
    }

    public void setCountryData(CountryDto countryData) {
        this.countryData = countryData;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}