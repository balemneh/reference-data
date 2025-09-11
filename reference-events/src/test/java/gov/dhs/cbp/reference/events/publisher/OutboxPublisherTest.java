package gov.dhs.cbp.reference.events.publisher;

import gov.dhs.cbp.reference.core.entity.OutboxEvent;
import gov.dhs.cbp.reference.core.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    private OutboxEvent outboxEvent;

    @BeforeEach
    void setUp() {
        outboxEvent = new OutboxEvent();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateId(UUID.randomUUID().toString());
        outboxEvent.setAggregateType("Country");
        outboxEvent.setEventType("CountryCreated");
        outboxEvent.setPayload("{\"countryName\":\"United States\",\"iso3Code\":\"USA\"}");
        outboxEvent.setCreatedAt(LocalDateTime.now());
        outboxEvent.setStatus(OutboxEvent.EventStatus.PENDING);
        outboxEvent.setRetryCount(0);
    }

    @Test
    void testPublishPendingEvents() {
        List<OutboxEvent> pendingEvents = Arrays.asList(outboxEvent);
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING))
                .thenReturn(pendingEvents);
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(future);

        outboxPublisher.publishPendingEvents();

        verify(outboxEventRepository).findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING);
        verify(kafkaTemplate).send(
                eq("reference-events"),
                eq(outboxEvent.getAggregateId()),
                eq(outboxEvent.getPayload())
        );
        // Verify event is saved twice (PROCESSING then PROCESSED)
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void testPublishPendingEventsWithError() {
        List<OutboxEvent> pendingEvents = Arrays.asList(outboxEvent);
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING))
                .thenReturn(pendingEvents);
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(future);

        outboxPublisher.publishPendingEvents();

        verify(outboxEventRepository).findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING);
        verify(kafkaTemplate).send(
                eq("reference-events"),
                eq(outboxEvent.getAggregateId()),
                eq(outboxEvent.getPayload())
        );
        // Verify event is saved twice (PROCESSING then back to PENDING with retry count)
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void testPublishPendingEventsWithMaxRetries() {
        outboxEvent.setRetryCount(3);
        List<OutboxEvent> pendingEvents = Arrays.asList(outboxEvent);
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING))
                .thenReturn(pendingEvents);
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(future);

        outboxPublisher.publishPendingEvents();

        verify(outboxEventRepository).findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING);
        verify(kafkaTemplate).send(
                eq("reference-events"),
                eq(outboxEvent.getAggregateId()),
                eq(outboxEvent.getPayload())
        );
        // Verify event is saved twice (PROCESSING then FAILED after max retries)
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void testPublishPendingEventsEmpty() {
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING))
                .thenReturn(Arrays.asList());

        outboxPublisher.publishPendingEvents();

        verify(outboxEventRepository).findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void testCreateAndPublishEvent() {
        String aggregateId = UUID.randomUUID().toString();
        String aggregateType = "Country";
        String eventType = "CountryUpdated";
        String payload = "{\"countryName\":\"Canada\"}";
        
        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        outboxPublisher.createEvent(aggregateId, aggregateType, eventType, payload);

        verify(outboxEventRepository).save(argThat(event ->
                event.getAggregateId().equals(aggregateId) &&
                event.getAggregateType().equals(aggregateType) &&
                event.getEventType().equals(eventType) &&
                event.getPayload().equals(payload) &&
                event.getStatus() == OutboxEvent.EventStatus.PENDING
        ));
    }
}