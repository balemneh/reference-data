package gov.dhs.cbp.reference.events.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.core.entity.*;
import gov.dhs.cbp.reference.events.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherServiceIntegrationTest {

    @Mock
    private OutboxPublisher outboxPublisher;

    private EventPublisherService eventPublisherService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        eventPublisherService = new EventPublisherService(outboxPublisher);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Test
    @DisplayName("Should publish Country CREATED event with all fields")
    void testPublishCountryCreatedEvent() {
        // Given
        Country country = createCountry();
        
        // When
        eventPublisherService.publishCountryEvent(
            country, 
            ReferenceDataEvent.EventType.CREATED, 
            "test-loader"
        );
        
        // Then
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
        
        assertEquals(country.getId().toString(), aggregateIdCaptor.getValue());
        assertEquals("Country", aggregateTypeCaptor.getValue());
        assertEquals("CREATED", eventTypeCaptor.getValue());
        
        // Verify payload contains expected data
        String payload = payloadCaptor.getValue();
        assertNotNull(payload);
        assertTrue(payload.contains("\"countryCode\":\"US\""));
        assertTrue(payload.contains("\"countryName\":\"United States\""));
    }

    @Test
    @DisplayName("Should publish Country UPDATED event")
    void testPublishCountryUpdatedEvent() {
        // Given
        Country country = createCountry();
        country.setVersion(2L);
        
        // When
        eventPublisherService.publishCountryEvent(
            country, 
            ReferenceDataEvent.EventType.UPDATED, 
            "test-updater"
        );
        
        // Then
        verify(outboxPublisher).createEvent(
            eq(country.getId().toString()),
            eq("Country"),
            eq("UPDATED"),
            anyString()
        );
    }

    @Test
    @DisplayName("Should publish Country DELETED event")
    void testPublishCountryDeletedEvent() {
        // Given
        Country country = createCountry();
        country.setValidTo(LocalDate.now());
        country.setIsActive(false);
        
        // When
        eventPublisherService.publishCountryEvent(
            country, 
            ReferenceDataEvent.EventType.DELETED, 
            "test-deleter"
        );
        
        // Then
        verify(outboxPublisher).createEvent(
            eq(country.getId().toString()),
            eq("Country"),
            eq("DELETED"),
            anyString()
        );
    }

    // TODO: Implement Airport entity and publishAirportEvent method before enabling this test
    /*
    @Test
    @DisplayName("Should publish Airport event with coordinates")
    void testPublishAirportEvent() {
        // Given
        Airport airport = createAirport();
        airport.setLatitude(BigDecimal.valueOf(33.6367));
        airport.setLongitude(BigDecimal.valueOf(-84.4281));
        airport.setElevationFt(1026);
        
        // When
        eventPublisherService.publishAirportEvent(
            airport, 
            ReferenceDataEvent.EventType.CREATED, 
            "test-loader"
        );
        
        // Then
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxPublisher).createEvent(
            eq(airport.getId().toString()),
            eq("Airport"),
            eq("CREATED"),
            payloadCaptor.capture()
        );
        
        String payload = payloadCaptor.getValue();
        assertTrue(payload.contains("\"iataCode\":\"ATL\""));
        assertTrue(payload.contains("\"icaoCode\":\"KATL\""));
        assertTrue(payload.contains("\"latitude\":33.6367"));
        assertTrue(payload.contains("\"longitude\":-84.4281"));
    }
    */

    // TODO: Implement Port entity and publishPortEvent method before enabling this test
    /*
    @Test
    @DisplayName("Should publish Port event with all fields")
    void testPublishPortEvent() {
        // Given
        Port port = createPort();
        port.setPortType("SEAPORT");
        port.setLatitude(BigDecimal.valueOf(40.7128));
        port.setLongitude(BigDecimal.valueOf(-74.0060));
        
        // When
        eventPublisherService.publishPortEvent(
            port, 
            ReferenceDataEvent.EventType.UPDATED, 
            "test-updater"
        );
        
        // Then
        verify(outboxPublisher).createEvent(
            eq(port.getId().toString()),
            eq("Port"),
            eq("UPDATED"),
            anyString()
        );
    }
    */

    // TODO: Implement Carrier entity and publishCarrierEvent method before enabling this test
    /*
    @Test
    @DisplayName("Should publish Carrier event")
    void testPublishCarrierEvent() {
        // Given
        Carrier carrier = createCarrier();
        carrier.setCarrierType("AIRLINE");
        
        // When
        eventPublisherService.publishCarrierEvent(
            carrier, 
            ReferenceDataEvent.EventType.CREATED, 
            "test-loader"
        );
        
        // Then
        verify(outboxPublisher).createEvent(
            eq(carrier.getId().toString()),
            eq("Carrier"),
            eq("CREATED"),
            anyString()
        );
    }
    */

    @Test
    @DisplayName("Should publish CodeMapping event with confidence")
    void testPublishCodeMappingEvent() {
        // Given
        CodeMapping mapping = createCodeMapping();
        mapping.setConfidence(BigDecimal.valueOf(95.5));
        mapping.setRuleId("RULE-123");
        
        // When
        eventPublisherService.publishCodeMappingEvent(
            mapping, 
            ReferenceDataEvent.EventType.CREATED, 
            "test-mapper"
        );
        
        // Then
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxPublisher).createEvent(
            eq(mapping.getId().toString()),
            eq("CodeMapping"),
            eq("CREATED"),
            payloadCaptor.capture()
        );
        
        String payload = payloadCaptor.getValue();
        assertTrue(payload.contains("\"confidence\":95.5"));
        assertTrue(payload.contains("\"ruleId\":\"RULE-123\""));
    }

    @Test
    @DisplayName("Should handle null CodeSystem gracefully")
    void testPublishEventWithNullCodeSystem() {
        // Given
        Country country = createCountry();
        country.setCodeSystem(null);
        
        // When
        eventPublisherService.publishCountryEvent(
            country, 
            ReferenceDataEvent.EventType.CREATED, 
            "test-loader"
        );
        
        // Then
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxPublisher).createEvent(
            anyString(),
            anyString(),
            anyString(),
            payloadCaptor.capture()
        );
        
        String payload = payloadCaptor.getValue();
        assertTrue(payload.contains("\"codeSystem\":null"));
    }

    @Test
    @DisplayName("Should set event metadata correctly")
    void testEventMetadata() throws Exception {
        // Given
        Country country = createCountry();
        
        // When
        eventPublisherService.publishCountryEvent(
            country, 
            ReferenceDataEvent.EventType.CREATED, 
            "test-user"
        );
        
        // Then
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxPublisher).createEvent(
            anyString(),
            anyString(),
            anyString(),
            payloadCaptor.capture()
        );
        
        // Parse the payload to verify metadata
        CountryChangedEvent event = objectMapper.readValue(
            payloadCaptor.getValue(), 
            CountryChangedEvent.class
        );
        
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
        assertEquals("test-user", event.getRecordedBy());
        assertEquals(1L, event.getVersion());
    }


    // TODO: Re-enable when Airport and Port entities are implemented
    /*
    @Test
    @DisplayName("Should publish multiple events in sequence")
    void testPublishMultipleEvents() {
        // Given
        Country country = createCountry();
        Airport airport = createAirport();
        Port port = createPort();
        
        // When
        eventPublisherService.publishCountryEvent(
            country, ReferenceDataEvent.EventType.CREATED, "loader"
        );
        eventPublisherService.publishAirportEvent(
            airport, ReferenceDataEvent.EventType.CREATED, "loader"
        );
        eventPublisherService.publishPortEvent(
            port, ReferenceDataEvent.EventType.CREATED, "loader"
        );
        
        // Then
        verify(outboxPublisher, times(3)).createEvent(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        );
    }
    */

    // Helper methods
    private Country createCountry() {
        Country country = new Country();
        country.setId(UUID.randomUUID());
        country.setVersion(1L);
        country.setCountryCode("US");
        country.setCountryName("United States");
        country.setIso2Code("US");
        country.setIso3Code("USA");
        country.setNumericCode("840");
        country.setValidFrom(LocalDate.now());
        country.setIsActive(true);
        
        CodeSystem codeSystem = new CodeSystem();
        codeSystem.setCode("ISO3166-1");
        country.setCodeSystem(codeSystem);
        
        return country;
    }

    // TODO: Re-enable when Airport entity is implemented
    /*
    private Airport createAirport() {
        Airport airport = new Airport();
        airport.setId(UUID.randomUUID());
        airport.setVersion(1L);
        airport.setIataCode("ATL");
        airport.setIcaoCode("KATL");
        airport.setAirportName("Atlanta International");
        airport.setCity("Atlanta");
        airport.setStateProvince("GA");
        airport.setCountryCode("US");
        airport.setValidFrom(LocalDate.now());
        return airport;
    }
    */

    // TODO: Re-enable when Port entity is implemented
    /*
    private Port createPort() {
        Port port = new Port();
        port.setId(UUID.randomUUID());
        port.setVersion(1L);
        port.setPortCode("2704");
        port.setPortName("New York");
        port.setCity("New York");
        port.setStateProvince("NY");
        port.setCountryCode("US");
        port.setValidFrom(LocalDate.now());
        return port;
    }
    */

    // TODO: Re-enable when Carrier entity is implemented
    /*
    private Carrier createCarrier() {
        Carrier carrier = new Carrier();
        carrier.setId(UUID.randomUUID());
        carrier.setVersion(1L);
        carrier.setCarrierCode("AA");
        carrier.setCarrierName("American Airlines");
        carrier.setIataCode("AA");
        carrier.setIcaoCode("AAL");
        carrier.setCountryCode("US");
        carrier.setValidFrom(LocalDate.now());
        return carrier;
    }
    */

    private CodeMapping createCodeMapping() {
        CodeMapping mapping = new CodeMapping();
        mapping.setId(UUID.randomUUID());
        mapping.setVersion(1L);
        mapping.setFromCode("US");
        mapping.setToCode("840");
        mapping.setMappingType("EXACT");
        mapping.setConfidence(BigDecimal.valueOf(100.0));
        mapping.setValidFrom(LocalDate.now());
        return mapping;
    }
}