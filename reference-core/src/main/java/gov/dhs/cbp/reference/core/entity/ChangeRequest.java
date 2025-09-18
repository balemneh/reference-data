package gov.dhs.cbp.reference.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "change_requests", schema = "reference_data",
       indexes = {
           @Index(name = "idx_change_request_status", columnList = "status"),
           @Index(name = "idx_change_request_requestor", columnList = "requester_id"),
           @Index(name = "idx_change_request_type", columnList = "change_type,entity_type"),
           @Index(name = "idx_change_request_created", columnList = "created_at")
       })
public class ChangeRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "cr_number", nullable = false, unique = true, length = 50)
    private String crNumber;

    @NotBlank
    @Size(max = 255)
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Size(max = 20)
    @Column(name = "operation_type", nullable = false, length = 20)
    private String operationType; // CREATE, UPDATE, DELETE, DEPRECATE
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType; // COUNTRY, PORT, AIRPORT, CARRIER, CODE_MAPPING
    
    @Column(name = "proposed_changes", columnDefinition = "jsonb")
    private String proposedChanges;
    
    @Column(name = "current_values", columnDefinition = "jsonb")
    private String currentValues;
    
    @NotBlank
    @Size(max = 20)
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, APPLIED, CANCELLED
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "requester_id", nullable = false, length = 100)
    private String requesterId;

    @Size(max = 100)
    @Column(name = "assignee_id", length = 100)
    private String assigneeId;
    
    @Size(max = 100)
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    
    @Column(name = "business_justification", columnDefinition = "TEXT")
    private String businessJustification;
    
    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    
    @Size(max = 10)
    @Column(name = "priority", length = 10)
    private String priority = "MEDIUM"; // HIGH, MEDIUM, LOW
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "implemented_at")
    private LocalDateTime implementedAt;

    @Column(name = "implemented_by", length = 100)
    private String implementedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "approval_data", columnDefinition = "jsonb")
    private String approvalData;
    
    
    @Column(name = "workflow_instance_id", length = 100)
    private String workflowInstanceId;
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
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
    
    public String getCrNumber() {
        return crNumber;
    }

    public void setCrNumber(String crNumber) {
        this.crNumber = crNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
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
    
    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
    
    public String getBusinessJustification() {
        return businessJustification;
    }

    public void setBusinessJustification(String businessJustification) {
        this.businessJustification = businessJustification;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
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
    
    public LocalDateTime getImplementedAt() {
        return implementedAt;
    }

    public void setImplementedAt(LocalDateTime implementedAt) {
        this.implementedAt = implementedAt;
    }

    public String getImplementedBy() {
        return implementedBy;
    }

    public void setImplementedBy(String implementedBy) {
        this.implementedBy = implementedBy;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public String getApprovalData() {
        return approvalData;
    }

    public void setApprovalData(String approvalData) {
        this.approvalData = approvalData;
    }
    
    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }
    
    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}