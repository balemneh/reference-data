package gov.dhs.cbp.reference.loader.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "loader")
public class LoaderConfiguration {
    
    private int batchSize = 1000;
    private boolean autoApplyChanges = false;
    private boolean failOnValidationError = false;
    private boolean publishEvents = true;
    private boolean incrementalMode = false;
    private int maxRetries = 3;
    private long retryDelayMillis = 5000;
    private String sourceUrl;
    private String sourceType = "FILE"; // FILE, HTTP, FTP, S3
    private boolean validateChecksums = true;
    private boolean enableScheduling = true;
    private String scheduleCron;
    private int connectionTimeoutMillis = 30000;
    private int readTimeoutMillis = 60000;
    
    // Getters and setters
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public boolean isAutoApplyChanges() {
        return autoApplyChanges;
    }
    
    public void setAutoApplyChanges(boolean autoApplyChanges) {
        this.autoApplyChanges = autoApplyChanges;
    }
    
    public boolean isFailOnValidationError() {
        return failOnValidationError;
    }
    
    public void setFailOnValidationError(boolean failOnValidationError) {
        this.failOnValidationError = failOnValidationError;
    }
    
    public boolean isPublishEvents() {
        return publishEvents;
    }
    
    public void setPublishEvents(boolean publishEvents) {
        this.publishEvents = publishEvents;
    }
    
    public boolean isIncrementalMode() {
        return incrementalMode;
    }
    
    public void setIncrementalMode(boolean incrementalMode) {
        this.incrementalMode = incrementalMode;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public long getRetryDelayMillis() {
        return retryDelayMillis;
    }
    
    public void setRetryDelayMillis(long retryDelayMillis) {
        this.retryDelayMillis = retryDelayMillis;
    }
    
    public String getSourceUrl() {
        return sourceUrl;
    }
    
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public boolean isValidateChecksums() {
        return validateChecksums;
    }
    
    public void setValidateChecksums(boolean validateChecksums) {
        this.validateChecksums = validateChecksums;
    }
    
    public boolean isEnableScheduling() {
        return enableScheduling;
    }
    
    public void setEnableScheduling(boolean enableScheduling) {
        this.enableScheduling = enableScheduling;
    }
    
    public String getScheduleCron() {
        return scheduleCron;
    }
    
    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }
    
    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }
    
    public void setConnectionTimeoutMillis(int connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }
    
    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }
    
    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }
}