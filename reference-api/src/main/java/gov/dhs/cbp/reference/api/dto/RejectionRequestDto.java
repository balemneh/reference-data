package gov.dhs.cbp.reference.api.dto;

import jakarta.validation.constraints.NotBlank;

public class RejectionRequestDto {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Reason is required")
    private String reason;

    // Default constructor
    public RejectionRequestDto() {
    }

    // Constructor with parameters
    public RejectionRequestDto(String userId, String reason) {
        this.userId = userId;
        this.reason = reason;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}