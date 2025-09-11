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
           @Index(name = "idx_change_request_requestor", columnList = "requestor"),
           @Index(name = "idx_change_request_type", columnList = "change_type,entity_type"),
           @Index(name = "idx_change_request_created", columnList = "created_at")
       })
public class ChangeRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "change_type", nullable = false, length = 100)
    private String changeType; // CREATE, UPDATE, DELETE, DEPRECATE
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType; // COUNTRY, PORT, AIRPORT, CARRIER, CODE_MAPPING
    
    @Column(name = "entity_id")
    private UUID entityId;
    
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
    @Column(name = "requestor", nullable = false, length = 100)
    private String requestor;
    
    @Size(max = 100)
    @Column(name = "approver", length = 100)
    private String approver;
    
    @Size(max = 500)
    @Column(name = "justification", length = 500)
    private String justification;
    
    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    
    @Column(name = "priority")
    private Integer priority = 3; // 1=High, 2=Medium, 3=Low, 4=Deferred
    
    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;
    
    @Column(name = "external_ticket_id", length = 100)
    private String externalTicketId;
    
    @Column(name = "workflow_instance_id", length = 100)
    private String workflowInstanceId;
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (effectiveDate == null) {
            effectiveDate = LocalDateTime.now();
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
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}