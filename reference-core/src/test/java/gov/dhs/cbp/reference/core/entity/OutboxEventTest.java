package gov.dhs.cbp.reference.core.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventTest {

    private OutboxEvent outboxEvent;

    @BeforeEach
    void setUp() {
        outboxEvent = new OutboxEvent();
    }

    @Test
    void testIdGenerationAndGetterSetter() {
        UUID id = UUID.randomUUID();
        outboxEvent.setId(id);
        assertEquals(id, outboxEvent.getId());
    }

    @Test
    void testAggregateIdGetterAndSetter() {
        String aggregateId = "country-123";
        outboxEvent.setAggregateId(aggregateId);
        assertEquals(aggregateId, outboxEvent.getAggregateId());
    }

    @Test
    void testAggregateTypeGetterAndSetter() {
        String aggregateType = "Country";
        outboxEvent.setAggregateType(aggregateType);
        assertEquals(aggregateType, outboxEvent.getAggregateType());
    }

    @Test
    void testEventTypeGetterAndSetter() {
        String eventType = "COUNTRY_CREATED";
        outboxEvent.setEventType(eventType);
        assertEquals(eventType, outboxEvent.getEventType());
    }

    @Test
    void testPayloadGetterAndSetter() {
        String payload = "{\"id\":\"123\",\"code\":\"US\",\"name\":\"United States\"}";
        outboxEvent.setPayload(payload);
        assertEquals(payload, outboxEvent.getPayload());
    }

    @Test
    void testCreatedAtGetterAndSetter() {
        LocalDateTime createdAt = LocalDateTime.now();
        outboxEvent.setCreatedAt(createdAt);
        assertEquals(createdAt, outboxEvent.getCreatedAt());
    }

    @Test
    void testProcessedAtGetterAndSetter() {
        LocalDateTime processedAt = LocalDateTime.now();
        outboxEvent.setProcessedAt(processedAt);
        assertEquals(processedAt, outboxEvent.getProcessedAt());
    }

    @Test
    void testStatusDefaultAndGetterSetter() {
        // Test default value
        assertEquals(OutboxEvent.EventStatus.PENDING, outboxEvent.getStatus());
        
        // Test all status values
        for (OutboxEvent.EventStatus status : OutboxEvent.EventStatus.values()) {
            outboxEvent.setStatus(status);
            assertEquals(status, outboxEvent.getStatus());
        }
    }

    @Test
    void testRetryCountDefaultAndGetterSetter() {
        // Test default value
        assertEquals(0, outboxEvent.getRetryCount());
        
        // Test setter
        Integer retryCount = 3;
        outboxEvent.setRetryCount(retryCount);
        assertEquals(retryCount, outboxEvent.getRetryCount());
    }

    @Test
    void testErrorMessageGetterAndSetter() {
        String errorMessage = "Failed to publish event to Kafka topic";
        outboxEvent.setErrorMessage(errorMessage);
        assertEquals(errorMessage, outboxEvent.getErrorMessage());
    }

    @Test
    void testEventStatusEnum() {
        // Test that all expected enum values exist
        OutboxEvent.EventStatus[] expectedStatuses = {
            OutboxEvent.EventStatus.PENDING,
            OutboxEvent.EventStatus.PROCESSING,
            OutboxEvent.EventStatus.PROCESSED,
            OutboxEvent.EventStatus.FAILED
        };
        
        OutboxEvent.EventStatus[] actualStatuses = OutboxEvent.EventStatus.values();
        assertEquals(expectedStatuses.length, actualStatuses.length);
        
        for (OutboxEvent.EventStatus expected : expectedStatuses) {
            boolean found = false;
            for (OutboxEvent.EventStatus actual : actualStatuses) {
                if (expected == actual) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Expected status " + expected + " not found");
        }
    }

    @Test
    void testCompleteOutboxEventWorkflow() {
        // Create a complete outbox event simulating a typical workflow
        UUID id = UUID.randomUUID();
        String aggregateId = "port-456";
        String aggregateType = "Port";
        String eventType = "PORT_UPDATED";
        String payload = "{\"id\":\"456\",\"code\":\"USNYC\",\"name\":\"New York Port\"}";
        LocalDateTime createdAt = LocalDateTime.now();
        
        outboxEvent.setId(id);
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setEventType(eventType);
        outboxEvent.setPayload(payload);
        outboxEvent.setCreatedAt(createdAt);
        outboxEvent.setStatus(OutboxEvent.EventStatus.PENDING);
        outboxEvent.setRetryCount(0);

        // Verify initial state
        assertEquals(id, outboxEvent.getId());
        assertEquals(aggregateId, outboxEvent.getAggregateId());
        assertEquals(aggregateType, outboxEvent.getAggregateType());
        assertEquals(eventType, outboxEvent.getEventType());
        assertEquals(payload, outboxEvent.getPayload());
        assertEquals(createdAt, outboxEvent.getCreatedAt());
        assertEquals(OutboxEvent.EventStatus.PENDING, outboxEvent.getStatus());
        assertEquals(0, outboxEvent.getRetryCount());
        assertNull(outboxEvent.getProcessedAt());
        assertNull(outboxEvent.getErrorMessage());
    }

    @Test
    void testOutboxEventStatusTransitions() {
        // Test typical status transition flow
        
        // Initial state
        assertEquals(OutboxEvent.EventStatus.PENDING, outboxEvent.getStatus());
        
        // Start processing
        outboxEvent.setStatus(OutboxEvent.EventStatus.PROCESSING);
        assertEquals(OutboxEvent.EventStatus.PROCESSING, outboxEvent.getStatus());
        
        // Successfully processed
        outboxEvent.setStatus(OutboxEvent.EventStatus.PROCESSED);
        outboxEvent.setProcessedAt(LocalDateTime.now());
        assertEquals(OutboxEvent.EventStatus.PROCESSED, outboxEvent.getStatus());
        assertNotNull(outboxEvent.getProcessedAt());
    }

    @Test
    void testOutboxEventFailureScenario() {
        // Test failure scenario with retries
        outboxEvent.setStatus(OutboxEvent.EventStatus.PROCESSING);
        outboxEvent.setRetryCount(1);
        
        // Simulate failure
        outboxEvent.setStatus(OutboxEvent.EventStatus.FAILED);
        outboxEvent.setErrorMessage("Connection timeout to Kafka broker");
        outboxEvent.setRetryCount(2);
        
        assertEquals(OutboxEvent.EventStatus.FAILED, outboxEvent.getStatus());
        assertEquals("Connection timeout to Kafka broker", outboxEvent.getErrorMessage());
        assertEquals(2, outboxEvent.getRetryCount());
    }

    @Test
    void testNullFieldHandling() {
        // Test that optional fields can be null
        outboxEvent.setProcessedAt(null);
        outboxEvent.setErrorMessage(null);
        
        assertNull(outboxEvent.getProcessedAt());
        assertNull(outboxEvent.getErrorMessage());
    }

    @Test
    void testLargePayloadHandling() {
        // Test handling of large JSON payload
        StringBuilder largePayload = new StringBuilder();
        largePayload.append("{\"id\":\"123\",\"data\":[");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) largePayload.append(",");
            largePayload.append("{\"item\":").append(i).append(",\"value\":\"data").append(i).append("\"}");
        }
        largePayload.append("]}");
        
        outboxEvent.setPayload(largePayload.toString());
        assertEquals(largePayload.toString(), outboxEvent.getPayload());
        assertTrue(outboxEvent.getPayload().length() > 10000);
    }

    @Test
    void testEventTypeVariations() {
        // Test different event types
        String[] eventTypes = {
            "COUNTRY_CREATED", "COUNTRY_UPDATED", "COUNTRY_DELETED",
            "PORT_CREATED", "PORT_UPDATED", "PORT_DELETED",
            "CARRIER_CREATED", "CARRIER_UPDATED", "CARRIER_DELETED",
            "MAPPING_CREATED", "MAPPING_UPDATED", "MAPPING_DELETED"
        };
        
        for (String eventType : eventTypes) {
            outboxEvent.setEventType(eventType);
            assertEquals(eventType, outboxEvent.getEventType());
        }
    }

    @Test
    void testAggregateTypeVariations() {
        // Test different aggregate types
        String[] aggregateTypes = {
            "Country", "Port", "Airport", "Carrier", "CodeMapping", "CodeSystem"
        };
        
        for (String aggregateType : aggregateTypes) {
            outboxEvent.setAggregateType(aggregateType);
            assertEquals(aggregateType, outboxEvent.getAggregateType());
        }
    }

    @Test
    void testRetryCountEdgeCases() {
        // Test negative retry count (should be allowed as it's just an integer)
        outboxEvent.setRetryCount(-1);
        assertEquals(-1, outboxEvent.getRetryCount());
        
        // Test high retry count
        outboxEvent.setRetryCount(100);
        assertEquals(100, outboxEvent.getRetryCount());
        
        // Test null retry count
        outboxEvent.setRetryCount(null);
        assertNull(outboxEvent.getRetryCount());
    }

    @Test
    void testTimestampComparisons() {
        // Test timestamp ordering
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime processedAt = createdAt.plusMinutes(5);
        
        outboxEvent.setCreatedAt(createdAt);
        outboxEvent.setProcessedAt(processedAt);
        
        assertTrue(outboxEvent.getProcessedAt().isAfter(outboxEvent.getCreatedAt()));
        assertTrue(outboxEvent.getCreatedAt().isBefore(outboxEvent.getProcessedAt()));
    }

    @Test
    void testOutboxEventDefaultValues() {
        OutboxEvent newEvent = new OutboxEvent();
        
        // Test default values
        assertEquals(OutboxEvent.EventStatus.PENDING, newEvent.getStatus());
        assertEquals(0, newEvent.getRetryCount());
        assertNull(newEvent.getId());
        assertNull(newEvent.getAggregateId());
        assertNull(newEvent.getAggregateType());
        assertNull(newEvent.getEventType());
        assertNull(newEvent.getPayload());
        assertNull(newEvent.getCreatedAt());
        assertNull(newEvent.getProcessedAt());
        assertNull(newEvent.getErrorMessage());
    }
}