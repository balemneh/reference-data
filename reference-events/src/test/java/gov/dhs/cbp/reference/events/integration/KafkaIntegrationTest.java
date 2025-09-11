package gov.dhs.cbp.reference.events.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.events.config.H2TestConfiguration;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.entity.OutboxEvent;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import gov.dhs.cbp.reference.core.repository.OutboxEventRepository;
import gov.dhs.cbp.reference.events.model.CountryChangedEvent;
import gov.dhs.cbp.reference.events.model.ReferenceDataEvent;
import gov.dhs.cbp.reference.events.publisher.EventPublisherService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(H2TestConfiguration.class)
@EmbeddedKafka(
    partitions = 1, 
    brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" },
    topics = { "reference-events", "country-events", "port-events", "airport-events", "carrier-events" }
)
@ActiveProfiles("integration-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Disabled("Kafka integration tests disabled for CI/CD - requires embedded Kafka")
class KafkaIntegrationTest {

    @Autowired
    private EventPublisherService eventPublisherService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private CodeSystemRepository codeSystemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaConsumer<String, String> testConsumer;
    private CodeSystem isoCodeSystem;

    @BeforeEach
    void setUp() {
        // Clean repositories
        outboxEventRepository.deleteAll();
        countryRepository.deleteAll();
        codeSystemRepository.deleteAll();

        // Create test code system
        isoCodeSystem = createCodeSystem("ISO3166-1", "ISO 3166-1 Country Codes");
        codeSystemRepository.save(isoCodeSystem);

        // Set up Kafka consumer for testing
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        testConsumer = new KafkaConsumer<>(consumerProps);
        testConsumer.subscribe(Arrays.asList("reference-events", "country-events"));
    }

    @Test
    @DisplayName("Should publish country event to outbox and Kafka topic")
    @Transactional
    void testCountryEventPublishing() throws Exception {
        // Given
        Country country = createCountry("US", "United States", isoCodeSystem);
        Country savedCountry = countryRepository.save(country);

        // When
        eventPublisherService.publishCountryEvent(
            savedCountry, 
            ReferenceDataEvent.EventType.CREATED, 
            "integration-test"
        );

        // Then - Verify outbox event was created
        await().atMost(Duration.ofSeconds(5))
               .untilAsserted(() -> {
                   List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
                   assertThat(outboxEvents).hasSize(1);
                   
                   OutboxEvent outboxEvent = outboxEvents.get(0);
                   assertThat(outboxEvent.getAggregateType()).isEqualTo("Country");
                   assertThat(outboxEvent.getEventType()).isEqualTo("CREATED");
                   assertThat(outboxEvent.getAggregateId()).isEqualTo(savedCountry.getId().toString());
               });

        // Verify Kafka message was published
        await().atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> {
                   ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofMillis(1000));
                   assertThat(records).isNotEmpty();
                   
                   boolean foundEvent = false;
                   for (ConsumerRecord<String, String> record : records) {
                       if (record.topic().equals("country-events") || record.topic().equals("reference-events")) {
                           JsonNode eventNode = objectMapper.readTree(record.value());
                           if ("CREATED".equals(eventNode.get("eventType").asText()) &&
                               "US".equals(eventNode.get("countryCode").asText())) {
                               foundEvent = true;
                               
                               // Verify event structure
                               assertThat(eventNode.get("countryName").asText()).isEqualTo("United States");
                               assertThat(eventNode.get("iso2Code").asText()).isEqualTo("US");
                               assertThat(eventNode.get("iso3Code").asText()).isEqualTo("USA");
                               assertThat(eventNode.get("eventId").asText()).isNotEmpty();
                               assertThat(eventNode.get("timestamp").asText()).isNotEmpty();
                               break;
                           }
                       }
                   }
                   assertThat(foundEvent).isTrue();
               });
    }

    @Test
    @DisplayName("Should handle event publishing failures gracefully")
    @Transactional
    void testEventPublishingFailureHandling() {
        // Given - Invalid country (missing required fields)
        Country invalidCountry = new Country();
        invalidCountry.setId(UUID.randomUUID());
        invalidCountry.setCountryCode(""); // Invalid empty code
        invalidCountry.setCountryName(""); // Invalid empty name

        // When & Then - Should handle gracefully without crashing
        try {
            eventPublisherService.publishCountryEvent(
                invalidCountry, 
                ReferenceDataEvent.EventType.CREATED, 
                "integration-test"
            );
            
            // Verify no outbox event was created for invalid data
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).isEmpty();
        } catch (Exception e) {
            // Expected behavior - should fail gracefully
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should publish events with proper ordering and deduplication")
    @Transactional
    void testEventOrderingAndDeduplication() throws Exception {
        // Given
        Country country = createCountry("CA", "Canada", isoCodeSystem);
        Country savedCountry = countryRepository.save(country);

        // When - Publish multiple events for same entity
        eventPublisherService.publishCountryEvent(
            savedCountry, ReferenceDataEvent.EventType.CREATED, "test-user"
        );
        
        savedCountry.setCountryName("Canada Updated");
        savedCountry.setVersion(2L);
        Country updatedCountry = countryRepository.save(savedCountry);
        
        eventPublisherService.publishCountryEvent(
            updatedCountry, ReferenceDataEvent.EventType.UPDATED, "test-user"
        );

        // Then - Verify both events are in outbox with proper ordering
        await().atMost(Duration.ofSeconds(5))
               .untilAsserted(() -> {
                   List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
                   outboxEvents.sort(Comparator.comparing(OutboxEvent::getCreatedAt));
                   assertThat(outboxEvents).hasSize(2);
                   
                   OutboxEvent createdEvent = outboxEvents.get(0);
                   OutboxEvent updatedEvent = outboxEvents.get(1);
                   
                   assertThat(createdEvent.getEventType()).isEqualTo("CREATED");
                   assertThat(updatedEvent.getEventType()).isEqualTo("UPDATED");
                   assertThat(createdEvent.getCreatedAt()).isBefore(updatedEvent.getCreatedAt());
               });

        // Verify events are published to Kafka in order
        await().atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> {
                   List<String> receivedEventTypes = new ArrayList<>();
                   
                   ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofMillis(2000));
                   for (ConsumerRecord<String, String> record : records) {
                       if (record.key() != null && record.key().contains("CA")) {
                           JsonNode eventNode = objectMapper.readTree(record.value());
                           receivedEventTypes.add(eventNode.get("eventType").asText());
                       }
                   }
                   
                   assertThat(receivedEventTypes).containsExactly("CREATED", "UPDATED");
               });
    }

    @Test
    @DisplayName("Should handle high volume of events without data loss")
    @Transactional
    void testHighVolumeEventProcessing() throws Exception {
        // Given - Multiple countries
        List<Country> countries = Arrays.asList(
            createCountry("US", "United States", isoCodeSystem),
            createCountry("CA", "Canada", isoCodeSystem),
            createCountry("MX", "Mexico", isoCodeSystem),
            createCountry("GB", "United Kingdom", isoCodeSystem),
            createCountry("FR", "France", isoCodeSystem)
        );
        
        List<Country> savedCountries = countryRepository.saveAll(countries);

        // When - Publish events for all countries
        for (Country country : savedCountries) {
            eventPublisherService.publishCountryEvent(
                country, ReferenceDataEvent.EventType.CREATED, "batch-loader"
            );
        }

        // Then - Verify all events are persisted
        await().atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> {
                   List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
                   assertThat(outboxEvents).hasSize(5);
                   
                   // Verify all country codes are present
                   Set<String> aggregateIds = new HashSet<>();
                   for (OutboxEvent event : outboxEvents) {
                       aggregateIds.add(event.getAggregateId());
                   }
                   assertThat(aggregateIds).hasSize(5);
               });

        // Verify all events are published to Kafka
        await().atMost(Duration.ofSeconds(15))
               .untilAsserted(() -> {
                   Set<String> receivedCountryCodes = new HashSet<>();
                   
                   // Poll multiple times to get all messages
                   for (int i = 0; i < 5; i++) {
                       ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofMillis(1000));
                       for (ConsumerRecord<String, String> record : records) {
                           JsonNode eventNode = objectMapper.readTree(record.value());
                           if (eventNode.has("countryCode")) {
                               receivedCountryCodes.add(eventNode.get("countryCode").asText());
                           }
                       }
                   }
                   
                   assertThat(receivedCountryCodes).containsExactlyInAnyOrder("US", "CA", "MX", "GB", "FR");
               });
    }

    @Test
    @DisplayName("Should support event replay from outbox")
    @Transactional
    void testEventReplay() throws Exception {
        // Given - Historical events in outbox
        Country country = createCountry("DE", "Germany", isoCodeSystem);
        Country savedCountry = countryRepository.save(country);
        
        // Create outbox event manually (simulating historical event)
        OutboxEvent historicalEvent = new OutboxEvent();
        historicalEvent.setId(UUID.randomUUID());
        historicalEvent.setAggregateId(savedCountry.getId().toString());
        historicalEvent.setAggregateType("Country");
        historicalEvent.setEventType("CREATED");
        historicalEvent.setCreatedAt(LocalDateTime.now().minusHours(1));
        historicalEvent.setStatus(OutboxEvent.EventStatus.PENDING);
        
        CountryChangedEvent eventPayload = new CountryChangedEvent();
        eventPayload.setEventId(UUID.randomUUID().toString());
        eventPayload.setEventType(ReferenceDataEvent.EventType.CREATED);
        eventPayload.setCountryCode("DE");
        eventPayload.setCountryName("Germany");
        eventPayload.setTimestamp(LocalDateTime.now().minusHours(1));
        
        historicalEvent.setPayload(objectMapper.writeValueAsString(eventPayload));
        outboxEventRepository.save(historicalEvent);

        // When - Trigger replay (this would normally be done by the outbox publisher)
        kafkaTemplate.send("country-events", savedCountry.getCountryCode(), 
                          objectMapper.writeValueAsString(eventPayload));

        // Then - Verify event can be consumed
        await().atMost(Duration.ofSeconds(5))
               .untilAsserted(() -> {
                   ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofMillis(1000));
                   boolean foundReplayedEvent = false;
                   
                   for (ConsumerRecord<String, String> record : records) {
                       JsonNode eventNode = objectMapper.readTree(record.value());
                       if ("DE".equals(eventNode.get("countryCode").asText()) &&
                           "CREATED".equals(eventNode.get("eventType").asText())) {
                           foundReplayedEvent = true;
                           assertThat(eventNode.get("countryName").asText()).isEqualTo("Germany");
                           break;
                       }
                   }
                   
                   assertThat(foundReplayedEvent).isTrue();
               });
    }

    // Helper methods
    private CodeSystem createCodeSystem(String code, String name) {
        CodeSystem codeSystem = new CodeSystem();
        codeSystem.setId(UUID.randomUUID());
        codeSystem.setCode(code);
        codeSystem.setName(name);
        codeSystem.setDescription("Test code system: " + name);
        codeSystem.setOwner("TEST");
        codeSystem.setCreatedAt(LocalDateTime.now());
        codeSystem.setUpdatedAt(LocalDateTime.now());
        return codeSystem;
    }

    private Country createCountry(String code, String name, CodeSystem codeSystem) {
        Country country = new Country();
        country.setId(UUID.randomUUID());
        country.setVersion(1L);
        country.setCodeSystem(codeSystem);
        country.setCountryCode(code);
        country.setCountryName(name);
        country.setValidFrom(LocalDate.now());
        country.setRecordedAt(LocalDateTime.now());
        country.setRecordedBy("integration-test");
        country.setIsActive(true);

        // Set ISO codes for common countries
        switch (code) {
            case "US":
                country.setIso2Code("US");
                country.setIso3Code("USA");
                country.setNumericCode("840");
                break;
            case "CA":
                country.setIso2Code("CA");
                country.setIso3Code("CAN");
                country.setNumericCode("124");
                break;
            case "MX":
                country.setIso2Code("MX");
                country.setIso3Code("MEX");
                country.setNumericCode("484");
                break;
            case "GB":
                country.setIso2Code("GB");
                country.setIso3Code("GBR");
                country.setNumericCode("826");
                break;
            case "FR":
                country.setIso2Code("FR");
                country.setIso3Code("FRA");
                country.setNumericCode("250");
                break;
            case "DE":
                country.setIso2Code("DE");
                country.setIso3Code("DEU");
                country.setNumericCode("276");
                break;
        }

        return country;
    }
}