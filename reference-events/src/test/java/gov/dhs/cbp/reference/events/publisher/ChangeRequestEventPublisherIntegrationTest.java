package gov.dhs.cbp.reference.events.publisher;

import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import gov.dhs.cbp.reference.core.entity.OutboxEvent;
import gov.dhs.cbp.reference.core.repository.ChangeRequestRepository;
import gov.dhs.cbp.reference.core.repository.OutboxEventRepository;
import gov.dhs.cbp.reference.events.config.KafkaTestConfiguration;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for Change Request Event Publishing.
 * Tests the complete flow from change request events to Kafka topic publication.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"change-requests", "country-changes", "reference-data-events"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChangeRequestEventPublisherIntegrationTest {

    private static final String CHANGE_REQUESTS_TOPIC = "change-requests";
    private static final String COUNTRY_CHANGES_TOPIC = "country-changes";
    private static final String REFERENCE_DATA_EVENTS_TOPIC = "reference-data-events";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private EventPublisherService eventPublisherService;

    @Autowired
    private ChangeRequestRepository changeRequestRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        // Set up Kafka consumer for test verification
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = consumerFactory.createConsumer();
        consumer.subscribe(Arrays.asList(CHANGE_REQUESTS_TOPIC, COUNTRY_CHANGES_TOPIC, REFERENCE_DATA_EVENTS_TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should publish change request created event to Kafka")
    void testPublishChangeRequestCreatedEvent() {
        // Given
        ChangeRequest changeRequest = createTestChangeRequest("CREATE");
        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);

        // When
        eventPublisherService.publishChangeRequestCreated(savedRequest);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();

            boolean foundCreatedEvent = false;
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals(CHANGE_REQUESTS_TOPIC)) {
                    String eventPayload = record.value();
                    assertThat(eventPayload).contains("\"eventType\":\"CHANGE_REQUEST_CREATED\"");
                    assertThat(eventPayload).contains("\"operationType\":\"CREATE\"");
                    assertThat(eventPayload).contains("\"dataType\":\"COUNTRY\"");
                    assertThat(eventPayload).contains(savedRequest.getId().toString());
                    foundCreatedEvent = true;
                    break;
                }
            }
            assertThat(foundCreatedEvent).isTrue();
        });

        // Verify outbox event was created and processed
        List<OutboxEvent> outboxEvents = outboxEventRepository.findByAggregateId(savedRequest.getId().toString());
        assertThat(outboxEvents).isNotEmpty();
        assertThat(outboxEvents.get(0).getEventType()).isEqualTo("CHANGE_REQUEST_CREATED");
        assertThat(outboxEvents.get(0).getStatus()).isEqualTo(OutboxEvent.EventStatus.PUBLISHED);
    }

    @Test
    @Order(2)
    @DisplayName("Should publish change request approved event to Kafka")
    void testPublishChangeRequestApprovedEvent() {
        // Given
        ChangeRequest changeRequest = createTestChangeRequest("APPROVE_TEST");
        changeRequest.setStatus("APPROVED");
        changeRequest.setApprovedBy("approver-123");
        changeRequest.setApprovedAt(LocalDateTime.now());
        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);

        // When
        eventPublisherService.publishChangeRequestApproved(savedRequest);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();

            boolean foundApprovedEvent = false;
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals(CHANGE_REQUESTS_TOPIC)) {
                    String eventPayload = record.value();
                    if (eventPayload.contains("\"eventType\":\"CHANGE_REQUEST_APPROVED\"")) {
                        assertThat(eventPayload).contains("\"status\":\"APPROVED\"");
                        assertThat(eventPayload).contains("\"approvedBy\":\"approver-123\"");
                        assertThat(eventPayload).contains(savedRequest.getId().toString());
                        foundApprovedEvent = true;
                        break;
                    }
                }
            }
            assertThat(foundApprovedEvent).isTrue();
        });
    }

    @Test
    @Order(3)
    @DisplayName("Should publish change request rejected event to Kafka")
    void testPublishChangeRequestRejectedEvent() {
        // Given
        ChangeRequest changeRequest = createTestChangeRequest("REJECT_TEST");
        changeRequest.setStatus("REJECTED");
        changeRequest.setRejectedBy("rejector-123");
        changeRequest.setRejectedAt(LocalDateTime.now());
        changeRequest.setRejectionReason("Business requirements not met");
        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);

        // When
        eventPublisherService.publishChangeRequestRejected(savedRequest);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();

            boolean foundRejectedEvent = false;
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals(CHANGE_REQUESTS_TOPIC)) {
                    String eventPayload = record.value();
                    if (eventPayload.contains("\"eventType\":\"CHANGE_REQUEST_REJECTED\"")) {
                        assertThat(eventPayload).contains("\"status\":\"REJECTED\"");
                        assertThat(eventPayload).contains("\"rejectedBy\":\"rejector-123\"");
                        assertThat(eventPayload).contains("\"rejectionReason\":\"Business requirements not met\"");
                        foundRejectedEvent = true;
                        break;
                    }
                }
            }
            assertThat(foundRejectedEvent).isTrue();
        });
    }

    @Test
    @Order(4)
    @DisplayName("Should publish change request implemented event to Kafka")
    void testPublishChangeRequestImplementedEvent() {
        // Given
        ChangeRequest changeRequest = createTestChangeRequest("IMPLEMENT_TEST");
        changeRequest.setStatus("APPLIED");
        changeRequest.setImplementedBy("SYSTEM");
        changeRequest.setImplementedAt(LocalDateTime.now());
        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);

        // When
        eventPublisherService.publishChangeRequestImplemented(savedRequest);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();

            boolean foundImplementedEvent = false;
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals(CHANGE_REQUESTS_TOPIC)) {
                    String eventPayload = record.value();
                    if (eventPayload.contains("\"eventType\":\"CHANGE_REQUEST_IMPLEMENTED\"")) {
                        assertThat(eventPayload).contains("\"status\":\"APPLIED\"");
                        assertThat(eventPayload).contains("\"implementedBy\":\"SYSTEM\"");
                        assertThat(eventPayload).contains(savedRequest.getId().toString());
                        foundImplementedEvent = true;
                        break;
                    }
                }
            }
            assertThat(foundImplementedEvent).isTrue();
        });
    }

    @Test
    @Order(5)
    @DisplayName("Should publish country data change event when change request is implemented")
    void testPublishCountryDataChangeEvent() {
        // Given
        ChangeRequest changeRequest = createTestChangeRequest("COUNTRY_CHANGE");
        changeRequest.setStatus("APPLIED");
        changeRequest.setOperationType("CREATE");
        changeRequest.setProposedChanges("""
            {
                "countryCode": "CC",
                "countryName": "Change Country",
                "iso2Code": "CC",
                "iso3Code": "CCH",
                "numericCode": "994",
                "codeSystem": "TEST-EVENTS",
                "isActive": true
            }
            """);
        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);

        // When
        eventPublisherService.publishCountryDataChange(savedRequest);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();

            boolean foundDataChangeEvent = false;
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals(COUNTRY_CHANGES_TOPIC)) {
                    String eventPayload = record.value();
                    assertThat(eventPayload).contains("\"eventType\":\"COUNTRY_UPSERT\"");
                    assertThat(eventPayload).contains("\"countryCode\":\"CC\"");
                    assertThat(eventPayload).contains("\"countryName\":\"Change Country\"");
                    assertThat(eventPayload).contains("\"changeRequestId\":");
                    foundDataChangeEvent = true;
                    break;
                }
            }
            assertThat(foundDataChangeEvent).isTrue();
        });
    }

    @Test
    @Order(6)
    @DisplayName("Should handle bulk change request events")
    void testPublishBulkChangeRequestEvents() {
        // Given
        List<ChangeRequest> bulkRequests = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            ChangeRequest request = createTestChangeRequest("BULK" + i);
            request.setStatus("APPROVED");
            request.setApprovedBy("bulk-approver");
            request.setApprovedAt(LocalDateTime.now());
            bulkRequests.add(changeRequestRepository.save(request));
        }

        // When
        eventPublisherService.publishBulkChangeRequestsApproved(bulkRequests);

        // Then
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();

            int approvedEventCount = 0;
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals(CHANGE_REQUESTS_TOPIC)) {
                    String eventPayload = record.value();
                    if (eventPayload.contains("\"eventType\":\"CHANGE_REQUEST_APPROVED\"") &&
                        eventPayload.contains("BULK")) {
                        approvedEventCount++;
                    }
                }
            }
            assertThat(approvedEventCount).isEqualTo(3);
        });

        // Verify all outbox events were created
        List<OutboxEvent> outboxEvents = new ArrayList<>();
        for (ChangeRequest request : bulkRequests) {
            outboxEvents.addAll(outboxEventRepository.findByAggregateId(request.getId().toString()));
        }
        assertThat(outboxEvents).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @Order(7)
    @DisplayName("Should ensure event ordering and idempotency")
    void testEventOrderingAndIdempotency() {
        // Given
        ChangeRequest changeRequest = createTestChangeRequest("ORDERING_TEST");
        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);

        // When - Publish multiple events for the same change request
        eventPublisherService.publishChangeRequestCreated(savedRequest);

        savedRequest.setStatus("APPROVED");
        savedRequest.setApprovedBy("approver");
        savedRequest.setApprovedAt(LocalDateTime.now());
        changeRequestRepository.save(savedRequest);
        eventPublisherService.publishChangeRequestApproved(savedRequest);

        savedRequest.setStatus("APPLIED");
        savedRequest.setImplementedBy("SYSTEM");
        savedRequest.setImplementedAt(LocalDateTime.now());
        changeRequestRepository.save(savedRequest);
        eventPublisherService.publishChangeRequestImplemented(savedRequest);

        // Then - Verify events are published in order
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            List<OutboxEvent> events = outboxEventRepository.findByAggregateIdOrderByCreatedAtAsc(
                savedRequest.getId().toString());
            assertThat(events).hasSizeGreaterThanOrEqualTo(3);

            assertThat(events.get(0).getEventType()).isEqualTo("CHANGE_REQUEST_CREATED");
            assertThat(events.get(1).getEventType()).isEqualTo("CHANGE_REQUEST_APPROVED");
            assertThat(events.get(2).getEventType()).isEqualTo("CHANGE_REQUEST_IMPLEMENTED");

            // Verify all events have unique sequence numbers
            Set<Long> sequenceNumbers = new HashSet<>();
            for (OutboxEvent event : events) {
                assertThat(sequenceNumbers.add(event.getSequenceNumber())).isTrue();
            }
        });
    }

    @Test
    @Order(8)
    @DisplayName("Should handle event publishing failures gracefully")
    void testEventPublishingFailureHandling() {
        // Given
        ChangeRequest changeRequest = createTestChangeRequest("FAILURE_TEST");
        changeRequest.setProposedChanges("invalid-json{"); // Malformed JSON to trigger failure
        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);

        // When - Try to publish event with invalid data
        assertThatThrownBy(() -> eventPublisherService.publishChangeRequestCreated(savedRequest))
            .isInstanceOf(Exception.class);

        // Then - Verify outbox event is marked as failed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<OutboxEvent> events = outboxEventRepository.findByAggregateId(savedRequest.getId().toString());
            if (!events.isEmpty()) {
                assertThat(events.get(0).getStatus()).isIn(
                    OutboxEvent.EventStatus.FAILED, OutboxEvent.EventStatus.PENDING);
            }
        });
    }

    @Test
    @Order(9)
    @DisplayName("Should support event replay functionality")
    void testEventReplayFunctionality() {
        // Given
        ChangeRequest changeRequest = createTestChangeRequest("REPLAY_TEST");
        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);

        // Create outbox event manually to simulate replay scenario
        OutboxEvent replayEvent = new OutboxEvent();
        replayEvent.setId(UUID.randomUUID());
        replayEvent.setAggregateId(savedRequest.getId().toString());
        replayEvent.setAggregateType("ChangeRequest");
        replayEvent.setEventType("CHANGE_REQUEST_CREATED");
        replayEvent.setEventData("""
            {
                "changeRequestId": "%s",
                "operationType": "CREATE",
                "dataType": "COUNTRY",
                "status": "PENDING"
            }
            """.formatted(savedRequest.getId()));
        replayEvent.setTopic(CHANGE_REQUESTS_TOPIC);
        replayEvent.setStatus(OutboxEvent.EventStatus.PENDING);
        replayEvent.setCreatedAt(LocalDateTime.now());
        replayEvent.setSequenceNumber(1L);
        outboxEventRepository.save(replayEvent);

        // When - Trigger replay (this would normally be done by a scheduled job)
        eventPublisherService.replayFailedEvents();

        // Then - Verify event was published
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            OutboxEvent updatedEvent = outboxEventRepository.findById(replayEvent.getId()).orElse(null);
            assertThat(updatedEvent).isNotNull();
            assertThat(updatedEvent.getStatus()).isEqualTo(OutboxEvent.EventStatus.PUBLISHED);
        });
    }

    @Test
    @Order(10)
    @DisplayName("Should publish events with correct message headers and metadata")
    void testEventMetadataAndHeaders() {
        // Given
        ChangeRequest changeRequest = createTestChangeRequest("METADATA_TEST");
        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);

        // When
        eventPublisherService.publishChangeRequestCreated(savedRequest);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();

            boolean foundEventWithMetadata = false;
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals(CHANGE_REQUESTS_TOPIC)) {
                    String eventPayload = record.value();
                    if (eventPayload.contains("METADATA_TEST")) {
                        // Verify event contains required metadata
                        assertThat(eventPayload).contains("\"timestamp\":");
                        assertThat(eventPayload).contains("\"version\":");
                        assertThat(eventPayload).contains("\"source\":\"reference-data-service\"");
                        assertThat(eventPayload).contains("\"correlationId\":");

                        // Verify headers are present
                        assertThat(record.headers()).isNotNull();
                        assertThat(record.key()).isEqualTo(savedRequest.getId().toString());

                        foundEventWithMetadata = true;
                        break;
                    }
                }
            }
            assertThat(foundEventWithMetadata).isTrue();
        });
    }

    // Helper methods
    private ChangeRequest createTestChangeRequest(String identifier) {
        ChangeRequest request = new ChangeRequest();
        request.setId(UUID.randomUUID());
        request.setCrNumber(generateCrNumber());
        request.setTitle("Test Change Request: " + identifier);
        request.setDescription("Integration test for event publishing: " + identifier);
        request.setOperationType("CREATE");
        request.setDataType("COUNTRY");
        request.setStatus("PENDING");
        request.setRequesterId("test-event-user");
        request.setBusinessJustification("Test justification for " + identifier);
        request.setPriority("MEDIUM");
        request.setCreatedAt(LocalDateTime.now());
        request.setSubmittedAt(LocalDateTime.now());
        request.setProposedChanges(String.format("""
            {
                "countryCode": "%s",
                "countryName": "%s Country",
                "iso2Code": "%s",
                "iso3Code": "%s",
                "numericCode": "993",
                "codeSystem": "TEST-EVENTS",
                "isActive": true
            }
            """, identifier, identifier, identifier.substring(0, Math.min(2, identifier.length())), identifier));
        return request;
    }

    private String generateCrNumber() {
        return String.format("CR-%d-%06d",
            LocalDateTime.now().getYear(),
            (int) (Math.random() * 1000000));
    }
}