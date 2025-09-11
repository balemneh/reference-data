package gov.dhs.cbp.reference.events.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.dhs.cbp.reference.core.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventModelsTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should create CountryChangedEvent from Country entity with all fields")
    void testCountryChangedEventFromEntity() {
        Country country = createSampleCountry();
        
        CountryChangedEvent event = CountryChangedEvent.fromEntity(
            country, ReferenceDataEvent.EventType.CREATED, "test-user"
        );
        
        // Verify base event fields
        assertEquals(country.getId().toString(), event.getAggregateId());
        assertEquals("Country", event.getAggregateType());
        assertEquals(ReferenceDataEvent.EventType.CREATED, event.getEventType());
        assertEquals("test-user", event.getRecordedBy());
        assertEquals(1L, event.getVersion());
        
        // Verify country-specific fields
        assertEquals("US", event.getCountryCode());
        assertEquals("United States", event.getCountryName());
        assertEquals("US", event.getIso2Code());
        assertEquals("USA", event.getIso3Code());
        assertEquals("840", event.getNumericCode());
        assertEquals(country.getValidFrom(), event.getValidFrom());
        assertNull(event.getValidTo());
        assertNull(event.getCodeSystem()); // Since we didn't set it
    }

    @Test
    @DisplayName("Should handle null CodeSystem in CountryChangedEvent")
    void testCountryChangedEventWithNullCodeSystem() {
        Country country = createSampleCountry();
        country.setCodeSystem(null);
        
        CountryChangedEvent event = CountryChangedEvent.fromEntity(
            country, ReferenceDataEvent.EventType.UPDATED, "test-user"
        );
        
        assertNull(event.getCodeSystem());
        assertNotNull(event.getAggregateId());
    }

    @Test
    @DisplayName("Should create CountryChangedEvent with CodeSystem")
    void testCountryChangedEventWithCodeSystem() {
        Country country = createSampleCountry();
        CodeSystem codeSystem = new CodeSystem();
        codeSystem.setCode("ISO3166-1");
        codeSystem.setName("ISO Country Codes");
        country.setCodeSystem(codeSystem);
        
        CountryChangedEvent event = CountryChangedEvent.fromEntity(
            country, ReferenceDataEvent.EventType.CREATED, "test-user"
        );
        
        assertEquals("ISO3166-1", event.getCodeSystem());
    }

    @Test
    @DisplayName("Should serialize and deserialize CountryChangedEvent correctly")
    void testCountryChangedEventSerialization() throws Exception {
        Country country = createSampleCountry();
        CountryChangedEvent event = CountryChangedEvent.fromEntity(
            country, ReferenceDataEvent.EventType.CREATED, "test-user"
        );
        
        // Set additional fields for complete testing
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(event);
        assertNotNull(json);
        assertTrue(json.contains("\"countryCode\":\"US\""));
        assertTrue(json.contains("\"countryName\":\"United States\""));
        
        // Deserialize back
        CountryChangedEvent deserialized = objectMapper.readValue(json, CountryChangedEvent.class);
        assertEquals(event.getCountryCode(), deserialized.getCountryCode());
        assertEquals(event.getCountryName(), deserialized.getCountryName());
        assertEquals(event.getAggregateId(), deserialized.getAggregateId());
    }

    // TODO: Implement Airport entity and AirportChangedEvent before enabling this test
    /*
    @Test
    @DisplayName("Should create AirportChangedEvent from Airport entity with all fields")
    void testAirportChangedEventFromEntity() {
        Airport airport = createSampleAirport();
        
        AirportChangedEvent event = AirportChangedEvent.fromEntity(
            airport, ReferenceDataEvent.EventType.UPDATED, "test-user"
        );
        
        // Verify base event fields
        assertEquals(airport.getId().toString(), event.getAggregateId());
        assertEquals("Airport", event.getAggregateType());
        assertEquals(ReferenceDataEvent.EventType.UPDATED, event.getEventType());
        assertEquals("test-user", event.getRecordedBy());
        
        // Verify airport-specific fields
        assertEquals("ATL", event.getIataCode());
        assertEquals("KATL", event.getIcaoCode());
        assertEquals("Atlanta International", event.getAirportName());
        assertEquals("Atlanta", event.getCity());
        assertEquals("GA", event.getStateProvince());
        assertEquals("US", event.getCountryCode());
        assertEquals(airport.getValidFrom(), event.getValidFrom());
    }
    */

    // TODO: Implement Airport entity and AirportChangedEvent before enabling this test
    /*
    @Test
    @DisplayName("Should serialize and deserialize AirportChangedEvent correctly")
    void testAirportChangedEventSerialization() throws Exception {
        Airport airport = createSampleAirport();
        airport.setLatitude(BigDecimal.valueOf(33.6367));
        airport.setLongitude(BigDecimal.valueOf(-84.4281));
        airport.setElevationFt(1026);
        
        AirportChangedEvent event = AirportChangedEvent.fromEntity(
            airport, ReferenceDataEvent.EventType.CREATED, "test-user"
        );
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(event);
        assertNotNull(json);
        assertTrue(json.contains("\"iataCode\":\"ATL\""));
        assertTrue(json.contains("\"airportName\":\"Atlanta International\""));
        
        // Deserialize back
        AirportChangedEvent deserialized = objectMapper.readValue(json, AirportChangedEvent.class);
        assertEquals(event.getIataCode(), deserialized.getIataCode());
        assertEquals(event.getAirportName(), deserialized.getAirportName());
        assertEquals(event.getLatitude(), deserialized.getLatitude());
        assertEquals(event.getLongitude(), deserialized.getLongitude());
    }
    */

    // TODO: Implement Port entity and PortChangedEvent before enabling this test
    /*
    @Test
    @DisplayName("Should create PortChangedEvent from Port entity with all fields")
    void testPortChangedEventFromEntity() {
        Port port = createSamplePort();
        port.setLatitude(BigDecimal.valueOf(40.7128));
        port.setLongitude(BigDecimal.valueOf(-74.0060));
        port.setPortType("SEAPORT");
        
        PortChangedEvent event = PortChangedEvent.fromEntity(
            port, ReferenceDataEvent.EventType.CREATED, "test-user"
        );
        
        // Verify base event fields
        assertEquals(port.getId().toString(), event.getAggregateId());
        assertEquals("Port", event.getAggregateType());
        assertEquals(ReferenceDataEvent.EventType.CREATED, event.getEventType());
        
        // Verify port-specific fields
        assertEquals("2704", event.getPortCode());
        assertEquals("New York", event.getPortName());
        assertEquals("New York", event.getCity());
        assertEquals("NY", event.getStateProvince());
        assertEquals("US", event.getCountryCode());
        assertEquals("SEAPORT", event.getPortType());
        assertEquals(port.getLatitude(), event.getLatitude());
        assertEquals(port.getLongitude(), event.getLongitude());
    }
    */

    // TODO: Implement Port entity and PortChangedEvent before enabling this test
    /*
    @Test
    @DisplayName("Should serialize and deserialize PortChangedEvent correctly")
    void testPortChangedEventSerialization() throws Exception {
        Port port = createSamplePort();
        PortChangedEvent event = PortChangedEvent.fromEntity(
            port, ReferenceDataEvent.EventType.UPDATED, "test-user"
        );
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());
        
        String json = objectMapper.writeValueAsString(event);
        assertNotNull(json);
        
        PortChangedEvent deserialized = objectMapper.readValue(json, PortChangedEvent.class);
        assertEquals(event.getPortCode(), deserialized.getPortCode());
        assertEquals(event.getPortName(), deserialized.getPortName());
    }
    */

    // TODO: Implement Carrier entity and CarrierChangedEvent before enabling this test
    /*
    @Test
    @DisplayName("Should create CarrierChangedEvent from Carrier entity with all fields")
    void testCarrierChangedEventFromEntity() {
        Carrier carrier = createSampleCarrier();
        carrier.setCarrierType("AIRLINE");
        
        CarrierChangedEvent event = CarrierChangedEvent.fromEntity(
            carrier, ReferenceDataEvent.EventType.DELETED, "test-user"
        );
        
        // Verify base event fields
        assertEquals(carrier.getId().toString(), event.getAggregateId());
        assertEquals("Carrier", event.getAggregateType());
        assertEquals(ReferenceDataEvent.EventType.DELETED, event.getEventType());
        
        // Verify carrier-specific fields
        assertEquals("AA", event.getCarrierCode());
        assertEquals("American Airlines", event.getCarrierName());
        assertEquals("AA", event.getIataCode());
        assertEquals("AAL", event.getIcaoCode());
        assertEquals("US", event.getCountryCode());
        assertEquals("AIRLINE", event.getCarrierType());
    }
    */

    @Test
    @DisplayName("Should create CodeMappingChangedEvent from CodeMapping entity")
    void testCodeMappingChangedEventFromEntity() {
        CodeMapping mapping = createSampleCodeMapping();
        mapping.setRuleId("RULE-001");
        mapping.setIsDeprecated(false);
        
        CodeMappingChangedEvent event = CodeMappingChangedEvent.fromEntity(
            mapping, ReferenceDataEvent.EventType.CREATED, "test-user"
        );
        
        // Verify base event fields
        assertEquals(mapping.getId().toString(), event.getAggregateId());
        assertEquals("CodeMapping", event.getAggregateType());
        
        // Verify mapping-specific fields
        assertEquals("US", event.getFromCode());
        assertEquals("840", event.getToCode());
        assertEquals("EXACT", event.getMappingType());
        assertEquals(BigDecimal.valueOf(100.0), event.getConfidence());
        assertEquals("RULE-001", event.getRuleId());
        assertFalse(event.getIsDeprecated());
    }

    @Test
    @DisplayName("Should handle CodeMapping with null CodeSystems")
    void testCodeMappingChangedEventWithNullSystems() {
        CodeMapping mapping = createSampleCodeMapping();
        mapping.setFromSystem(null);
        mapping.setToSystem(null);
        
        CodeMappingChangedEvent event = CodeMappingChangedEvent.fromEntity(
            mapping, ReferenceDataEvent.EventType.UPDATED, "test-user"
        );
        
        assertNull(event.getFromSystem());
        assertNull(event.getToSystem());
        assertEquals("US", event.getFromCode());
        assertEquals("840", event.getToCode());
    }

    @Test
    @DisplayName("Should handle all event types correctly")
    void testAllEventTypes() {
        Country country = createSampleCountry();
        
        // Test CREATED event
        CountryChangedEvent createdEvent = CountryChangedEvent.fromEntity(
            country, ReferenceDataEvent.EventType.CREATED, "creator"
        );
        assertEquals(ReferenceDataEvent.EventType.CREATED, createdEvent.getEventType());
        
        // Test UPDATED event
        CountryChangedEvent updatedEvent = CountryChangedEvent.fromEntity(
            country, ReferenceDataEvent.EventType.UPDATED, "updater"
        );
        assertEquals(ReferenceDataEvent.EventType.UPDATED, updatedEvent.getEventType());
        
        // Test DELETED event
        CountryChangedEvent deletedEvent = CountryChangedEvent.fromEntity(
            country, ReferenceDataEvent.EventType.DELETED, "deleter"
        );
        assertEquals(ReferenceDataEvent.EventType.DELETED, deletedEvent.getEventType());
    }

    // Helper methods to create sample entities
    private Country createSampleCountry() {
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
        return country;
    }

    // TODO: Re-enable when Airport entity is implemented
    /*
    private Airport createSampleAirport() {
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
    private Port createSamplePort() {
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
    private Carrier createSampleCarrier() {
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

    private CodeMapping createSampleCodeMapping() {
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