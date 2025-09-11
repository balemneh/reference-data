package gov.dhs.cbp.reference.loader.common;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Base class for all staging table entities
 */
@MappedSuperclass
public abstract class StagingEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "load_execution_id", nullable = false, length = 36)
    private String loadExecutionId;
    
    @Column(name = "loaded_at", nullable = false)
    private LocalDateTime loadedAt;
    
    @Column(name = "source_hash", length = 64)
    private String sourceHash;
    
    @Column(name = "validation_status", length = 20)
    @Enumerated(EnumType.STRING)
    private ValidationStatus validationStatus = ValidationStatus.PENDING;
    
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;
    
    @Column(name = "processing_status", length = 20)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;
    
    @Column(name = "processing_notes", columnDefinition = "TEXT")
    private String processingNotes;
    
    public enum ValidationStatus {
        PENDING, VALID, INVALID, WARNING
    }
    
    public enum ProcessingStatus {
        PENDING, PROCESSED, SKIPPED, FAILED
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getLoadExecutionId() {
        return loadExecutionId;
    }
    
    public void setLoadExecutionId(String loadExecutionId) {
        this.loadExecutionId = loadExecutionId;
    }
    
    public LocalDateTime getLoadedAt() {
        return loadedAt;
    }
    
    public void setLoadedAt(LocalDateTime loadedAt) {
        this.loadedAt = loadedAt;
    }
    
    public String getSourceHash() {
        return sourceHash;
    }
    
    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
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
    
    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }
    
    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }
    
    public String getProcessingNotes() {
        return processingNotes;
    }
    
    public void setProcessingNotes(String processingNotes) {
        this.processingNotes = processingNotes;
    }
}