package gov.dhs.cbp.reference.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChangeRequestDto {

    private UUID id;

    private String crNumber;

    @NotBlank
    @Size(max = 100)
    private String changeType;

    private String operationType;

    private String title;

    private String submittedBy;

    @NotBlank
    @Size(max = 100)
    private String entityType;
    
    private UUID entityId;
    
    private String proposedChanges;
    
    private String currentValues;
    
    @NotBlank
    @Size(max = 20)
    private String status = "PENDING";
    
    @NotBlank
    @Size(max = 100)
    private String requestor;
    
    @Size(max = 100)
    private String approver;
    
    @Size(max = 500)
    private String justification;
    
    @Size(max = 500)
    private String rejectionReason;
    
    private Integer priority = 3;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime effectiveDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime approvedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime appliedAt;
    
    @Size(max = 100)
    private String externalTicketId;
    
    @Size(max = 100)
    private String workflowInstanceId;
    
    // Helper methods
    public boolean isPending() {
        return "PENDING".equals(status);
    }
    
    public boolean isApproved() {
        return "APPROVED".equals(status);
    }
    
    public boolean isRejected() {
        return "REJECTED".equals(status);
    }
    
    public boolean isApplied() {
        return "APPLIED".equals(status);
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getChangeType() {
        return changeType;
    }
    
    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public UUID getEntityId() {
        return entityId;
    }
    
    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }
    
    public String getProposedChanges() {
        return proposedChanges;
    }
    
    public void setProposedChanges(String proposedChanges) {
        this.proposedChanges = proposedChanges;
    }
    
    public String getCurrentValues() {
        return currentValues;
    }
    
    public void setCurrentValues(String currentValues) {
        this.currentValues = currentValues;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getRequestor() {
        return requestor;
    }
    
    public void setRequestor(String requestor) {
        this.requestor = requestor;
    }
    
    public String getApprover() {
        return approver;
    }
    
    public void setApprover(String approver) {
        this.approver = approver;
    }
    
    public String getJustification() {
        return justification;
    }
    
    public void setJustification(String justification) {
        this.justification = justification;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public LocalDateTime getEffectiveDate() {
        return effectiveDate;
    }
    
    public void setEffectiveDate(LocalDateTime effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
    
    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }
    
    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
    
    public String getExternalTicketId() {
        return externalTicketId;
    }
    
    public void setExternalTicketId(String externalTicketId) {
        this.externalTicketId = externalTicketId;
    }
    
    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    public String getCrNumber() {
        return crNumber;
    }

    public void setCrNumber(String crNumber) {
        this.crNumber = crNumber;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }
}