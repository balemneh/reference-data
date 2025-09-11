package gov.dhs.cbp.reference.loader.common;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Context object passed through the loader execution
 */
public class LoaderContext {
    
    private String executionId;
    private String userId;
    private String changeRequestId;
    private boolean incrementalMode = false;
    private LocalDateTime lastRunTime;
    private Map<String, Object> metadata = new HashMap<>();
    private LoadType loadType = LoadType.FULL;
    private boolean dryRun = false;
    
    public enum LoadType {
        FULL, INCREMENTAL, DELTA, SNAPSHOT
    }
    
    public LoaderContext() {
    }
    
    public LoaderContext(String executionId, String userId) {
        this.executionId = executionId;
        this.userId = userId;
    }
    
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    // Getters and setters
    public String getExecutionId() {
        return executionId;
    }
    
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getChangeRequestId() {
        return changeRequestId;
    }
    
    public void setChangeRequestId(String changeRequestId) {
        this.changeRequestId = changeRequestId;
    }
    
    public boolean isIncrementalMode() {
        return incrementalMode;
    }
    
    public void setIncrementalMode(boolean incrementalMode) {
        this.incrementalMode = incrementalMode;
    }
    
    public LocalDateTime getLastRunTime() {
        return lastRunTime;
    }
    
    public void setLastRunTime(LocalDateTime lastRunTime) {
        this.lastRunTime = lastRunTime;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public LoadType getLoadType() {
        return loadType;
    }
    
    public void setLoadType(LoadType loadType) {
        this.loadType = loadType;
    }
    
    public boolean isDryRun() {
        return dryRun;
    }
    
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}