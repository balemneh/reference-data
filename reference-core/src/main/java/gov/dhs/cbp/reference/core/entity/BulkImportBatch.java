package gov.dhs.cbp.reference.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_import_batches", schema = "reference_data",
       indexes = {
           @Index(name = "idx_batch_change_request", columnList = "change_request_id"),
           @Index(name = "idx_batch_data_type_status", columnList = "data_type,status"),
           @Index(name = "idx_batch_source_system", columnList = "source_system,created_at"),
           @Index(name = "idx_batch_created_at", columnList = "created_at")
       })
public class BulkImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "batch_name", nullable = false)
    private String batchName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "change_request_id", nullable = false)
    private UUID changeRequestId;

    // Source information
    @NotBlank
    @Size(max = 100)
    @Column(name = "source_system", nullable = false)
    private String sourceSystem;

    @Size(max = 500)
    @Column(name = "source_file_name")
    private String sourceFileName;

    @Column(name = "source_file_size")
    private Long sourceFileSize;

    @Size(max = 64)
    @Column(name = "source_file_checksum")
    private String sourceFileChecksum;

    @Size(max = 50)
    @Column(name = "source_file_format")
    private String sourceFileFormat;

    // Data type and processing configuration
    @NotBlank
    @Size(max = 50)
    @Column(name = "data_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DataType dataType;

    @NotBlank
    @Size(max = 50)
    @Column(name = "import_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private ImportMode importMode = ImportMode.STANDARD;

    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private String validationRules;

    @Column(name = "transformation_config", columnDefinition = "jsonb")
    private String transformationConfig;

    @Column(name = "business_rules_config", columnDefinition = "jsonb")
    private String businessRulesConfig;

    // Status and progress tracking
    @NotBlank
    @Size(max = 20)
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BatchStatus status = BatchStatus.PENDING;

    @Column(name = "total_records")
    private Integer totalRecords = 0;

    @Column(name = "records_processed")
    private Integer recordsProcessed = 0;

    @Column(name = "records_valid")
    private Integer recordsValid = 0;

    @Column(name = "records_invalid")
    private Integer recordsInvalid = 0;

    @Column(name = "records_warnings")
    private Integer recordsWarnings = 0;

    @Column(name = "records_skipped")
    private Integer recordsSkipped = 0;

    // Quality metrics
    @Column(name = "overall_quality_score", precision = 5, scale = 2)
    private BigDecimal overallQualityScore;

    @Column(name = "validation_pass_rate", precision = 5, scale = 2)
    private BigDecimal validationPassRate;

    @Column(name = "processing_success_rate", precision = 5, scale = 2)
    private BigDecimal processingSuccessRate;

    // Timing information
    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotBlank
    @Size(max = 100)
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // Error and notification tracking
    @Column(name = "error_summary", columnDefinition = "jsonb")
    private String errorSummary;

    @Column(name = "notification_config", columnDefinition = "jsonb")
    private String notificationConfig;

    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    // Metadata
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    // Foreign key relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", insertable = false, updatable = false)
    private ChangeRequest changeRequest;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Enums
    public enum DataType {
        COUNTRIES, AIRPORTS, PORTS, CARRIERS, LANGUAGES, CURRENCIES, UNITS, CODE_MAPPINGS
    }

    public enum ImportMode {
        STANDARD, INCREMENTAL, FULL_REPLACE, MERGE, VALIDATION_ONLY
    }

    public enum BatchStatus {
        PENDING, VALIDATING, PROCESSING, COMPLETED, FAILED, CANCELLED
    }

    // Utility methods
    public boolean isCompleted() {
        return BatchStatus.COMPLETED.equals(status);
    }

    public boolean isFailed() {
        return BatchStatus.FAILED.equals(status);
    }

    public boolean isInProgress() {
        return BatchStatus.VALIDATING.equals(status) || BatchStatus.PROCESSING.equals(status);
    }

    public double getProgressPercentage() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        return (double) (recordsProcessed != null ? recordsProcessed : 0) / totalRecords * 100.0;
    }

    public void updateProgress() {
        if (totalRecords != null && totalRecords > 0) {
            // Calculate validation pass rate
            int validRecords = (recordsValid != null ? recordsValid : 0);
            int invalidRecords = (recordsInvalid != null ? recordsInvalid : 0);
            int totalValidated = validRecords + invalidRecords;

            if (totalValidated > 0) {
                validationPassRate = BigDecimal.valueOf((double) validRecords / totalValidated * 100.0);
            }

            // Calculate processing success rate
            int processed = (recordsProcessed != null ? recordsProcessed : 0);
            if (processed > 0) {
                processingSuccessRate = BigDecimal.valueOf((double) processed / totalRecords * 100.0);
            }
        }
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getChangeRequestId() {
        return changeRequestId;
    }

    public void setChangeRequestId(UUID changeRequestId) {
        this.changeRequestId = changeRequestId;
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

    public Long getSourceFileSize() {
        return sourceFileSize;
    }

    public void setSourceFileSize(Long sourceFileSize) {
        this.sourceFileSize = sourceFileSize;
    }

    public String getSourceFileChecksum() {
        return sourceFileChecksum;
    }

    public void setSourceFileChecksum(String sourceFileChecksum) {
        this.sourceFileChecksum = sourceFileChecksum;
    }

    public String getSourceFileFormat() {
        return sourceFileFormat;
    }

    public void setSourceFileFormat(String sourceFileFormat) {
        this.sourceFileFormat = sourceFileFormat;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public ImportMode getImportMode() {
        return importMode;
    }

    public void setImportMode(ImportMode importMode) {
        this.importMode = importMode;
    }

    public String getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(String validationRules) {
        this.validationRules = validationRules;
    }

    public String getTransformationConfig() {
        return transformationConfig;
    }

    public void setTransformationConfig(String transformationConfig) {
        this.transformationConfig = transformationConfig;
    }

    public String getBusinessRulesConfig() {
        return businessRulesConfig;
    }

    public void setBusinessRulesConfig(String businessRulesConfig) {
        this.businessRulesConfig = businessRulesConfig;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public Integer getRecordsValid() {
        return recordsValid;
    }

    public void setRecordsValid(Integer recordsValid) {
        this.recordsValid = recordsValid;
    }

    public Integer getRecordsInvalid() {
        return recordsInvalid;
    }

    public void setRecordsInvalid(Integer recordsInvalid) {
        this.recordsInvalid = recordsInvalid;
    }

    public Integer getRecordsWarnings() {
        return recordsWarnings;
    }

    public void setRecordsWarnings(Integer recordsWarnings) {
        this.recordsWarnings = recordsWarnings;
    }

    public Integer getRecordsSkipped() {
        return recordsSkipped;
    }

    public void setRecordsSkipped(Integer recordsSkipped) {
        this.recordsSkipped = recordsSkipped;
    }

    public BigDecimal getOverallQualityScore() {
        return overallQualityScore;
    }

    public void setOverallQualityScore(BigDecimal overallQualityScore) {
        this.overallQualityScore = overallQualityScore;
    }

    public BigDecimal getValidationPassRate() {
        return validationPassRate;
    }

    public void setValidationPassRate(BigDecimal validationPassRate) {
        this.validationPassRate = validationPassRate;
    }

    public BigDecimal getProcessingSuccessRate() {
        return processingSuccessRate;
    }

    public void setProcessingSuccessRate(BigDecimal processingSuccessRate) {
        this.processingSuccessRate = processingSuccessRate;
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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    public String getNotificationConfig() {
        return notificationConfig;
    }

    public void setNotificationConfig(String notificationConfig) {
        this.notificationConfig = notificationConfig;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public ChangeRequest getChangeRequest() {
        return changeRequest;
    }

    public void setChangeRequest(ChangeRequest changeRequest) {
        this.changeRequest = changeRequest;
    }
}