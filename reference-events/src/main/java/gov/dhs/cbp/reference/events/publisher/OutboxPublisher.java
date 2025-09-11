package gov.dhs.cbp.reference.events.publisher;

import gov.dhs.cbp.reference.core.entity.OutboxEvent;
import gov.dhs.cbp.reference.core.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class OutboxPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int MAX_RETRIES = 3;
    
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public OutboxPublisher(OutboxEventRepository outboxEventRepository, 
                          KafkaTemplate<String, Object> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING);
        
        for (OutboxEvent event : pendingEvents) {
            publishEvent(event);
        }
    }
    
    private void publishEvent(OutboxEvent event) {
        try {
            event.setStatus(OutboxEvent.EventStatus.PROCESSING);
            outboxEventRepository.save(event);
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    "reference-events",
                    event.getAggregateId(),
                    event.getPayload()
            );
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    event.setStatus(OutboxEvent.EventStatus.PROCESSED);
                    event.setProcessedAt(LocalDateTime.now());
                    outboxEventRepository.save(event);
                    logger.info("Successfully published event {} for aggregate {}", 
                            event.getEventType(), event.getAggregateId());
                } else {
                    handleFailure(event, ex);
                }
            });
        } catch (Exception e) {
            handleFailure(event, e);
        }
    }
    
    private void handleFailure(OutboxEvent event, Throwable ex) {
        event.setRetryCount(event.getRetryCount() + 1);
        
        if (event.getRetryCount() > MAX_RETRIES) {
            event.setStatus(OutboxEvent.EventStatus.FAILED);
            event.setErrorMessage(ex.getMessage());
            logger.error("Failed to publish event {} after {} retries", 
                    event.getId(), MAX_RETRIES, ex);
        } else {
            event.setStatus(OutboxEvent.EventStatus.PENDING);
            logger.warn("Failed to publish event {}, will retry (attempt {})", 
                    event.getId(), event.getRetryCount(), ex);
        }
        
        outboxEventRepository.save(event);
    }
    
    @Transactional
    public void createEvent(String aggregateId, String aggregateType, 
                           String eventType, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(aggregateId);
        event.setAggregateType(aggregateType);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setStatus(OutboxEvent.EventStatus.PENDING);
        event.setCreatedAt(LocalDateTime.now());
        event.setRetryCount(0);
        
        outboxEventRepository.save(event);
        logger.debug("Created outbox event {} for aggregate {}", eventType, aggregateId);
    }
}