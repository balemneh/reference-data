package gov.dhs.cbp.reference.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ScheduledExportDto {
    private UUID id;
    private String name;
    private String entityType;
    private String format;
    private String schedule;
    private boolean enabled;
    private String filters;
    private LocalDateTime lastRun;
    private LocalDateTime nextRun;
    private String createdBy;
    private String recipients;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and setters...
    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEntityType() {
        return entityType;
    }
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }
    public String getSchedule() {
        return schedule;
    }
    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public String getFilters() {
        return filters;
    }
    public void setFilters(String filters) {
        this.filters = filters;
    }
    public LocalDateTime getLastRun() {
        return lastRun;
    }
    public void setLastRun(LocalDateTime lastRun) {
        this.lastRun = lastRun;
    }
    public LocalDateTime getNextRun() {
        return nextRun;
    }
    public void setNextRun(LocalDateTime nextRun) {
        this.nextRun = nextRun;
    }
    public String getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    public String getRecipients() {
        return recipients;
    }
    public void setRecipients(String recipients) {
        this.recipients = recipients;
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
}
