package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus status);
    
    List<OutboxEvent> findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            OutboxEvent.EventStatus status, int maxRetries);
    
    long countByStatus(OutboxEvent.EventStatus status);
}