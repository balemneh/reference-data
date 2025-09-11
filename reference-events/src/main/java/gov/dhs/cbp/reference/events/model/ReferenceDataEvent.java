package gov.dhs.cbp.reference.events.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Base event model for reference data changes
 */
public abstract class ReferenceDataEvent {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("eventType")
    private EventType eventType;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("aggregateId")
    private String aggregateId;

    @JsonProperty("aggregateType")
    private String aggregateType;

    @JsonProperty("version")
    private Long version;

    @JsonProperty("changeRequestId")
    private String changeRequestId;

    @JsonProperty("recordedBy")
    private String recordedBy;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED,
        DEPRECATED,
        MAPPING_CREATED,
        MAPPING_UPDATED,
        MAPPING_DEPRECATED
    }

    // Getters and setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getChangeRequestId() {
        return changeRequestId;
    }

    public void setChangeRequestId(String changeRequestId) {
        this.changeRequestId = changeRequestId;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}