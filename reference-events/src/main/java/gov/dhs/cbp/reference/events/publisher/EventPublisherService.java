package gov.dhs.cbp.reference.events.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.dhs.cbp.reference.events.model.*;
import gov.dhs.cbp.reference.core.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for publishing reference data events via the transactional outbox pattern
 */
@Service
public class EventPublisherService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventPublisherService.class);
    
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public EventPublisherService(OutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Publish events for entity changes from loaders
     */
    public void publishCountryEvent(Country country, ReferenceDataEvent.EventType eventType, String recordedBy) {
        CountryChangedEvent event = CountryChangedEvent.fromEntity(country, eventType, recordedBy);
        publishCountryEvent(event);
    }


    public void publishCodeMappingEvent(CodeMapping mapping, ReferenceDataEvent.EventType eventType, String recordedBy) {
        CodeMappingChangedEvent event = CodeMappingChangedEvent.fromEntity(mapping, eventType, recordedBy);
        publishEvent(event);
    }

    /**
     * Publish change request lifecycle events
     */
    public void publishChangeRequestCreated(ChangeRequest changeRequest) {
        publishChangeRequestEvent(changeRequest, "CHANGE_REQUEST_CREATED");
    }

    public void publishChangeRequestApproved(ChangeRequest changeRequest) {
        publishChangeRequestEvent(changeRequest, "CHANGE_REQUEST_APPROVED");
    }

    public void publishChangeRequestRejected(ChangeRequest changeRequest) {
        publishChangeRequestEvent(changeRequest, "CHANGE_REQUEST_REJECTED");
    }

    public void publishChangeRequestCompleted(ChangeRequest changeRequest) {
        publishChangeRequestEvent(changeRequest, "CHANGE_REQUEST_COMPLETED");
    }

    private void publishChangeRequestEvent(ChangeRequest changeRequest, String eventType) {
        try {
            // Create event payload
            var eventPayload = new java.util.HashMap<String, Object>();
            eventPayload.put("eventType", eventType);
            eventPayload.put("eventId", UUID.randomUUID().toString());
            eventPayload.put("timestamp", LocalDateTime.now());
            eventPayload.put("changeRequestId", changeRequest.getId());
            eventPayload.put("crNumber", changeRequest.getCrNumber());
            eventPayload.put("title", changeRequest.getTitle());
            eventPayload.put("dataType", changeRequest.getDataType());
            eventPayload.put("operationType", changeRequest.getOperationType());
            eventPayload.put("status", changeRequest.getStatus());
            eventPayload.put("requesterId", changeRequest.getRequesterId());

            String payload = objectMapper.writeValueAsString(eventPayload);

            outboxPublisher.createEvent(
                changeRequest.getId().toString(),
                "ChangeRequest",
                eventType,
                payload
            );

            logger.info("Published {} event for change request {}", eventType, changeRequest.getCrNumber());

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize change request event for {}: {}",
                    changeRequest.getCrNumber(), e.getMessage(), e);
            throw new EventPublishException("Failed to serialize change request event", e);
        }
    }

    /**
     * Publish a country changed event
     */
    public void publishCountryEvent(CountryChangedEvent event) {
        try {
            // Set event metadata
            event.setEventId(UUID.randomUUID().toString());
            event.setTimestamp(LocalDateTime.now());
            
            String payload = objectMapper.writeValueAsString(event);
            
            outboxPublisher.createEvent(
                event.getAggregateId(),
                event.getAggregateType(),
                event.getEventType().name(),
                payload
            );
            
            logger.info("Published {} event for country {} ({})", 
                    event.getEventType(), event.getCountryCode(), event.getAggregateId());
                    
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event for country {}: {}", 
                    event.getAggregateId(), e.getMessage(), e);
            throw new EventPublishException("Failed to serialize country event", e);
        }
    }
    
    /**
     * Publish a generic reference data event
     */
    public void publishEvent(ReferenceDataEvent event) {
        try {
            // Set event metadata if not already set
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }
            if (event.getTimestamp() == null) {
                event.setTimestamp(LocalDateTime.now());
            }
            
            String payload = objectMapper.writeValueAsString(event);
            
            outboxPublisher.createEvent(
                event.getAggregateId(),
                event.getAggregateType(), 
                event.getEventType().name(),
                payload
            );
            
            logger.info("Published {} event for {} ({})", 
                    event.getEventType(), event.getAggregateType(), event.getAggregateId());
                    
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event for {} {}: {}", 
                    event.getAggregateType(), event.getAggregateId(), e.getMessage(), e);
            throw new EventPublishException("Failed to serialize reference data event", e);
        }
    }
    
    public static class EventPublishException extends RuntimeException {
        public EventPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}