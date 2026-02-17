package gov.dhs.cbp.reference.api.dto;

import jakarta.validation.constraints.NotBlank;

public class ApprovalRequestDto {

    @NotBlank(message = "User ID is required")
    private String userId;

    private String comments;

    // Default constructor
    public ApprovalRequestDto() {
    }

    // Constructor with parameters
    public ApprovalRequestDto(String userId, String comments) {
        this.userId = userId;
        this.comments = comments;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}