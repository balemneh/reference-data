package gov.dhs.cbp.reference.events.publisher;

import gov.dhs.cbp.reference.events.model.CountryChangedEvent;
import gov.dhs.cbp.reference.events.model.ReferenceDataEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherServiceTest {
    
    @Mock
    private OutboxPublisher outboxPublisher;
    
    @InjectMocks
    private EventPublisherService eventPublisherService;
    
    private CountryChangedEvent countryEvent;
    
    @BeforeEach
    void setUp() {
        countryEvent = new CountryChangedEvent();
        countryEvent.setAggregateId("12345");
        countryEvent.setCountryCode("US");
        countryEvent.setCountryName("United States");
        countryEvent.setCodeSystem("ISO3166-1");
        countryEvent.setEventType(ReferenceDataEvent.EventType.CREATED);
        countryEvent.setRecordedBy("test-user");
    }
    
    @Test
    void testPublishCountryEvent() {
        eventPublisherService.publishCountryEvent(countryEvent);
        
        ArgumentCaptor<String> aggregateIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aggregateTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(outboxPublisher).createEvent(
            aggregateIdCaptor.capture(),
            aggregateTypeCaptor.capture(), 
            eventTypeCaptor.capture(),
            payloadCaptor.capture()
        );
        
        assertEquals("12345", aggregateIdCaptor.getValue());
        assertEquals("Country", aggregateTypeCaptor.getValue());
        assertEquals("CREATED", eventTypeCaptor.getValue());
        
        String payload = payloadCaptor.getValue();
        assertTrue(payload.contains("\"countryCode\":\"US\""));
        assertTrue(payload.contains("\"countryName\":\"United States\""));
        assertTrue(payload.contains("\"codeSystem\":\"ISO3166-1\""));
    }
    
    @Test
    void testPublishCountryEventSetsEventMetadata() {
        // Event starts without metadata
        assertNull(countryEvent.getEventId());
        assertNull(countryEvent.getTimestamp());
        
        eventPublisherService.publishCountryEvent(countryEvent);
        
        // Should now have metadata set
        assertNotNull(countryEvent.getEventId());
        assertNotNull(countryEvent.getTimestamp());
        
        verify(outboxPublisher).createEvent(anyString(), anyString(), anyString(), anyString());
    }
    
    @Test
    void testPublishGenericEvent() {
        ReferenceDataEvent genericEvent = new ReferenceDataEvent() {
            // Anonymous subclass for testing
        };
        genericEvent.setAggregateId("67890");
        genericEvent.setAggregateType("Port");
        genericEvent.setEventType(ReferenceDataEvent.EventType.UPDATED);
        
        eventPublisherService.publishEvent(genericEvent);
        
        ArgumentCaptor<String> aggregateIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aggregateTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(outboxPublisher).createEvent(
            aggregateIdCaptor.capture(),
            aggregateTypeCaptor.capture(),
            eventTypeCaptor.capture(),
            anyString()
        );
        
        assertEquals("67890", aggregateIdCaptor.getValue());
        assertEquals("Port", aggregateTypeCaptor.getValue());
        assertEquals("UPDATED", eventTypeCaptor.getValue());
    }
    
    @Test
    void testPublishGenericEventSetsMetadataIfMissing() {
        ReferenceDataEvent genericEvent = new ReferenceDataEvent() {
            // Anonymous subclass for testing
        };
        genericEvent.setAggregateId("67890");
        genericEvent.setAggregateType("Port");
        genericEvent.setEventType(ReferenceDataEvent.EventType.UPDATED);
        
        // Initially no metadata
        assertNull(genericEvent.getEventId());
        assertNull(genericEvent.getTimestamp());
        
        eventPublisherService.publishEvent(genericEvent);
        
        // Should now have metadata
        assertNotNull(genericEvent.getEventId());
        assertNotNull(genericEvent.getTimestamp());
        
        verify(outboxPublisher).createEvent(anyString(), anyString(), anyString(), anyString());
    }
}