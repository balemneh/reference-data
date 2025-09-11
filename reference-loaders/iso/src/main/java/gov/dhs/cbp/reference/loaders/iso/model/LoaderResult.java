package gov.dhs.cbp.reference.loaders.iso.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoaderResult {
    private final String loaderName;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private int recordsProcessed = 0;
    private int recordsCreated = 0;
    private int recordsUpdated = 0;
    private int recordsFailed = 0;
    private List<String> errors = new ArrayList<>();
    private String status = "IN_PROGRESS";
    
    public LoaderResult(String loaderName) {
        this.loaderName = loaderName;
        this.startTime = LocalDateTime.now();
    }
    
    public void complete() {
        this.endTime = LocalDateTime.now();
        this.status = errors.isEmpty() ? "SUCCESS" : "SUCCESS_WITH_ERRORS";
    }
    
    public void fail(String error) {
        this.endTime = LocalDateTime.now();
        this.status = "FAILED";
        this.errors.add(error);
    }
    
    public void incrementProcessed() {
        this.recordsProcessed++;
    }
    
    public void incrementCreated() {
        this.recordsCreated++;
    }
    
    public void incrementUpdated() {
        this.recordsUpdated++;
    }
    
    public void incrementFailed() {
        this.recordsFailed++;
    }
    
    public void addError(String error) {
        this.errors.add(error);
    }
    
    // Getters
    public String getLoaderName() {
        return loaderName;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public int getRecordsProcessed() {
        return recordsProcessed;
    }
    
    public int getRecordsCreated() {
        return recordsCreated;
    }
    
    public int getRecordsUpdated() {
        return recordsUpdated;
    }
    
    public int getRecordsFailed() {
        return recordsFailed;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public String getStatus() {
        return status;
    }
    
    @Override
    public String toString() {
        return String.format("LoaderResult[%s: %s, processed=%d, created=%d, updated=%d, failed=%d]",
                loaderName, status, recordsProcessed, recordsCreated, recordsUpdated, recordsFailed);
    }
}