package gov.dhs.cbp.reference.core.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
public abstract class Bitemporal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @Column(name = "version", nullable = false)
    @Version
    private Long version;
    
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;
    
    @Column(name = "valid_to")
    private LocalDate validTo;
    
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
    
    @Column(name = "recorded_by", nullable = false, length = 100)
    private String recordedBy;
    
    @Column(name = "change_request_id", length = 100)
    private String changeRequestId;
    
    @Column(name = "is_correction", nullable = false)
    private Boolean isCorrection = false;
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
    
    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
        if (validFrom == null) {
            validFrom = LocalDate.now();
        }
        if (version == null) {
            version = 1L;
        }
    }
    
    public boolean isCurrentlyValid() {
        LocalDate now = LocalDate.now();
        return !now.isBefore(validFrom) && (validTo == null || now.isBefore(validTo));
    }
    
    public boolean wasValidOn(LocalDate date) {
        return !date.isBefore(validFrom) && (validTo == null || date.isBefore(validTo));
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
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
    
    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
    
    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
    
    public String getRecordedBy() {
        return recordedBy;
    }
    
    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }
    
    public String getChangeRequestId() {
        return changeRequestId;
    }
    
    public void setChangeRequestId(String changeRequestId) {
        this.changeRequestId = changeRequestId;
    }
    
    public Boolean getIsCorrection() {
        return isCorrection;
    }
    
    public void setIsCorrection(Boolean isCorrection) {
        this.isCorrection = isCorrection;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}