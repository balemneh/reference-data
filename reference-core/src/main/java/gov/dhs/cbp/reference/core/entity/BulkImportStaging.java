package gov.dhs.cbp.reference.core.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_import_staging", schema = "reference_data",
       indexes = {
           @Index(name = "idx_staging_batch_id", columnList = "import_batch_id"),
           @Index(name = "idx_staging_change_request", columnList = "change_request_id"),
           @Index(name = "idx_staging_data_type_status", columnList = "data_type,processing_status"),
           @Index(name = "idx_staging_validation_status", columnList = "validation_status,processing_status"),
           @Index(name = "idx_staging_natural_key", columnList = "data_type,natural_key,import_batch_id"),
           @Index(name = "idx_staging_source_system", columnList = "source_system,created_at"),
           @Index(name = "idx_staging_target_record", columnList = "target_record_id,target_version"),
           @Index(name = "idx_staging_manual_review", columnList = "requires_manual_review,processing_status,created_at"),
           @Index(name = "idx_staging_created_at", columnList = "created_at"),
           @Index(name = "idx_staging_processed_at", columnList = "processed_at")
       })
public class BulkImportStaging {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "import_batch_id", nullable = false)
    private UUID importBatchId;

    @NotNull
    @Column(name = "change_request_id", nullable = false)
    private UUID changeRequestId;

    // Data type and operation information
    @NotNull
    @Column(name = "data_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DataType dataType;

    @NotNull
    @Column(name = "operation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    @NotBlank
    @Size(max = 100)
    @Column(name = "source_system", nullable = false)
    private String sourceSystem;

    @Size(max = 500)
    @Column(name = "source_file_name")
    private String sourceFileName;

    @Size(max = 64)
    @Column(name = "source_file_checksum")
    private String sourceFileChecksum;

    // Record identification
    @NotNull
    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Size(max = 100)
    @Column(name = "source_record_id")
    private String sourceRecordId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "natural_key", nullable = false)
    private String naturalKey;

    // Raw and normalized data
    @NotBlank
    @Type(JsonType.class)
    @Column(name = "raw_data", nullable = false, columnDefinition = "jsonb")
    private String rawData;

    @Type(JsonType.class)
    @Column(name = "normalized_data", columnDefinition = "jsonb")
    private String normalizedData;

    @NotBlank
    @Size(max = 100)
    @Column(name = "target_table", nullable = false)
    private String targetTable;

    // Validation and processing status
    @Column(name = "validation_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    @Type(JsonType.class)
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    private String validationErrors;

    @Type(JsonType.class)
    @Column(name = "validation_warnings", columnDefinition = "jsonb")
    private String validationWarnings;

    @NotNull
    @Column(name = "processing_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Type(JsonType.class)
    @Column(name = "processing_errors", columnDefinition = "jsonb")
    private String processingErrors;

    // Target record information
    @Column(name = "target_record_id")
    private UUID targetRecordId;

    @Column(name = "target_version")
    private Long targetVersion;

    @Column(name = "conflict_resolution")
    @Enumerated(EnumType.STRING)
    private ConflictResolution conflictResolution;

    @Column(name = "merge_strategy")
    @Enumerated(EnumType.STRING)
    private MergeStrategy mergeStrategy;

    // Quality scores and confidence
    @Column(name = "data_quality_score", precision = 5, scale = 2)
    private BigDecimal dataQualityScore;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "requires_manual_review")
    private Boolean requiresManualReview = false;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    // Audit trail
    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotBlank
    @Size(max = 100)
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @NotBlank
    @Size(max = 100)
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    // Processing timestamps
    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Size(max = 100)
    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Size(max = 100)
    @Column(name = "processed_by")
    private String processedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Size(max = 100)
    @Column(name = "approved_by")
    private String approvedBy;

    // Metadata and lineage
    @Type(JsonType.class)
    @Column(name = "data_lineage", columnDefinition = "jsonb")
    private String dataLineage;

    @Type(JsonType.class)
    @Column(name = "transformation_rules", columnDefinition = "jsonb")
    private String transformationRules;

    @Type(JsonType.class)
    @Column(name = "business_rules_applied", columnDefinition = "jsonb")
    private String businessRulesApplied;

    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    // Foreign key relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_batch_id", insertable = false, updatable = false)
    private BulkImportBatch importBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", insertable = false, updatable = false)
    private ChangeRequest changeRequest;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Enums
    public enum DataType {
        COUNTRIES, AIRPORTS, PORTS, CARRIERS, LANGUAGES, CURRENCIES, UNITS, CODE_MAPPINGS
    }

    public enum OperationType {
        INSERT, UPDATE, DELETE, UPSERT, MERGE
    }

    public enum ValidationStatus {
        PENDING, VALID, INVALID, WARNING, SKIPPED
    }

    public enum ProcessingStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, SKIPPED, REQUIRES_REVIEW
    }

    public enum ConflictResolution {
        OVERWRITE, MERGE, SKIP, MANUAL_REVIEW
    }

    public enum MergeStrategy {
        LAST_WINS, FIRST_WINS, MERGE_FIELDS, MANUAL_REVIEW
    }

    // Utility methods
    public boolean isValid() {
        return ValidationStatus.VALID.equals(validationStatus);
    }

    public boolean isProcessed() {
        return ProcessingStatus.COMPLETED.equals(processingStatus);
    }

    public boolean hasFailed() {
        return ProcessingStatus.FAILED.equals(processingStatus);
    }

    public boolean requiresReview() {
        return requiresManualReview || ProcessingStatus.REQUIRES_REVIEW.equals(processingStatus);
    }

    public void markAsValidated(String validatedBy) {
        this.validationStatus = ValidationStatus.VALID;
        this.validatedAt = LocalDateTime.now();
        this.validatedBy = validatedBy;
    }

    public void markAsInvalid(String validationErrors, String validatedBy) {
        this.validationStatus = ValidationStatus.INVALID;
        this.validationErrors = validationErrors;
        this.validatedAt = LocalDateTime.now();
        this.validatedBy = validatedBy;
    }

    public void markAsProcessed(String processedBy) {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = processedBy;
    }

    public void markAsFailed(String processingErrors, String processedBy) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.processingErrors = "{\"error\": \"" + processingErrors + "\"}";
        this.processedAt = LocalDateTime.now();
        this.processedBy = processedBy;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getImportBatchId() {
        return importBatchId;
    }

    public void setImportBatchId(UUID importBatchId) {
        this.importBatchId = importBatchId;
    }

    public UUID getChangeRequestId() {
        return changeRequestId;
    }

    public void setChangeRequestId(UUID changeRequestId) {
        this.changeRequestId = changeRequestId;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getSourceFileChecksum() {
        return sourceFileChecksum;
    }

    public void setSourceFileChecksum(String sourceFileChecksum) {
        this.sourceFileChecksum = sourceFileChecksum;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getSourceRecordId() {
        return sourceRecordId;
    }

    public void setSourceRecordId(String sourceRecordId) {
        this.sourceRecordId = sourceRecordId;
    }

    public String getNaturalKey() {
        return naturalKey;
    }

    public void setNaturalKey(String naturalKey) {
        this.naturalKey = naturalKey;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public String getNormalizedData() {
        return normalizedData;
    }

    public void setNormalizedData(String normalizedData) {
        this.normalizedData = normalizedData;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(String validationErrors) {
        this.validationErrors = validationErrors;
    }

    public String getValidationWarnings() {
        return validationWarnings;
    }

    public void setValidationWarnings(String validationWarnings) {
        this.validationWarnings = validationWarnings;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getProcessingErrors() {
        return processingErrors;
    }

    public void setProcessingErrors(String processingErrors) {
        this.processingErrors = processingErrors;
    }

    public UUID getTargetRecordId() {
        return targetRecordId;
    }

    public void setTargetRecordId(UUID targetRecordId) {
        this.targetRecordId = targetRecordId;
    }

    public Long getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(Long targetVersion) {
        this.targetVersion = targetVersion;
    }

    public ConflictResolution getConflictResolution() {
        return conflictResolution;
    }

    public void setConflictResolution(ConflictResolution conflictResolution) {
        this.conflictResolution = conflictResolution;
    }

    public MergeStrategy getMergeStrategy() {
        return mergeStrategy;
    }

    public void setMergeStrategy(MergeStrategy mergeStrategy) {
        this.mergeStrategy = mergeStrategy;
    }

    public BigDecimal getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(BigDecimal dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Boolean getRequiresManualReview() {
        return requiresManualReview;
    }

    public void setRequiresManualReview(Boolean requiresManualReview) {
        this.requiresManualReview = requiresManualReview;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }

    public String getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(String validatedBy) {
        this.validatedBy = validatedBy;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getDataLineage() {
        return dataLineage;
    }

    public void setDataLineage(String dataLineage) {
        this.dataLineage = dataLineage;
    }

    public String getTransformationRules() {
        return transformationRules;
    }

    public void setTransformationRules(String transformationRules) {
        this.transformationRules = transformationRules;
    }

    public String getBusinessRulesApplied() {
        return businessRulesApplied;
    }

    public void setBusinessRulesApplied(String businessRulesApplied) {
        this.businessRulesApplied = businessRulesApplied;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public BulkImportBatch getImportBatch() {
        return importBatch;
    }

    public void setImportBatch(BulkImportBatch importBatch) {
        this.importBatch = importBatch;
    }

    public ChangeRequest getChangeRequest() {
        return changeRequest;
    }

    public void setChangeRequest(ChangeRequest changeRequest) {
        this.changeRequest = changeRequest;
    }
}