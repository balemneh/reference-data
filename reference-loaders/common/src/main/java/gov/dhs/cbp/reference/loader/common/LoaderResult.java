package gov.dhs.cbp.reference.loader.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoaderResult {
    private String executionId;
    private String loaderName;
    private LoaderStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMillis;
    
    private Integer recordsRead = 0;
    private Integer recordsStaged = 0;
    private Integer recordsAdded = 0;
    private Integer recordsUpdated = 0;
    private Integer recordsDeleted = 0;
    private Integer recordsSkipped = 0;
    
    private List<ValidationError> validationErrors = new ArrayList<>();
    private String errorMessage;
    private String changeRequestId;
    private boolean changesApplied = false;
    
    public LoaderResult(String executionId, String loaderName) {
        this.executionId = executionId;
        this.loaderName = loaderName;
        this.startTime = LocalDateTime.now();
        this.status = LoaderStatus.IN_PROGRESS;
    }
    
    public boolean hasErrors() {
        return !validationErrors.isEmpty() || errorMessage != null;
    }
    
    public int getTotalChanges() {
        return recordsAdded + recordsUpdated + recordsDeleted;
    }
    
    public double getSuccessRate() {
        if (recordsRead == 0) return 0.0;
        return (double) recordsStaged / recordsRead * 100;
    }
    
    // Getters and setters
    public String getExecutionId() {
        return executionId;
    }
    
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    
    public String getLoaderName() {
        return loaderName;
    }
    
    public void setLoaderName(String loaderName) {
        this.loaderName = loaderName;
    }
    
    public LoaderStatus getStatus() {
        return status;
    }
    
    public void setStatus(LoaderStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Long getDurationMillis() {
        return durationMillis;
    }
    
    public void setDurationMillis(Long durationMillis) {
        this.durationMillis = durationMillis;
    }
    
    public Integer getRecordsRead() {
        return recordsRead;
    }
    
    public void setRecordsRead(Integer recordsRead) {
        this.recordsRead = recordsRead;
    }
    
    public Integer getRecordsStaged() {
        return recordsStaged;
    }
    
    public void setRecordsStaged(Integer recordsStaged) {
        this.recordsStaged = recordsStaged;
    }
    
    public Integer getRecordsAdded() {
        return recordsAdded;
    }
    
    public void setRecordsAdded(Integer recordsAdded) {
        this.recordsAdded = recordsAdded;
    }
    
    public Integer getRecordsUpdated() {
        return recordsUpdated;
    }
    
    public void setRecordsUpdated(Integer recordsUpdated) {
        this.recordsUpdated = recordsUpdated;
    }
    
    public Integer getRecordsDeleted() {
        return recordsDeleted;
    }
    
    public void setRecordsDeleted(Integer recordsDeleted) {
        this.recordsDeleted = recordsDeleted;
    }
    
    public Integer getRecordsSkipped() {
        return recordsSkipped;
    }
    
    public void setRecordsSkipped(Integer recordsSkipped) {
        this.recordsSkipped = recordsSkipped;
    }
    
    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getChangeRequestId() {
        return changeRequestId;
    }
    
    public void setChangeRequestId(String changeRequestId) {
        this.changeRequestId = changeRequestId;
    }
    
    public boolean isChangesApplied() {
        return changesApplied;
    }
    
    public void setChangesApplied(boolean changesApplied) {
        this.changesApplied = changesApplied;
    }
    
    @Override
    public String toString() {
        return String.format(
            "LoaderResult{loader='%s', status=%s, read=%d, staged=%d, added=%d, updated=%d, deleted=%d, duration=%dms}",
            loaderName, status, recordsRead, recordsStaged, recordsAdded, recordsUpdated, recordsDeleted, durationMillis
        );
    }
}