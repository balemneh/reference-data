package gov.dhs.cbp.reference.api.service;

import gov.dhs.cbp.reference.api.dto.ChangeRequestDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.mapper.ChangeRequestMapper;
import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import gov.dhs.cbp.reference.core.repository.ChangeRequestRepository;
import gov.dhs.cbp.reference.events.publisher.OutboxPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChangeRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChangeRequestService.class);
    
    private final ChangeRequestRepository changeRequestRepository;
    private final ChangeRequestMapper changeRequestMapper;
    private final OutboxPublisher outboxPublisher;
    
    public ChangeRequestService(ChangeRequestRepository changeRequestRepository,
                               ChangeRequestMapper changeRequestMapper,
                               @Autowired(required = false) OutboxPublisher outboxPublisher) {
        this.changeRequestRepository = changeRequestRepository;
        this.changeRequestMapper = changeRequestMapper;
        this.outboxPublisher = outboxPublisher;
    }
    
    public Optional<ChangeRequestDto> findById(UUID id) {
        return changeRequestRepository.findById(id)
                .map(changeRequestMapper::toDto);
    }
    
    public PagedResponse<ChangeRequestDto> findByFilters(String status, String requestor, 
                                                        String entityType, String changeType,
                                                        LocalDateTime fromDate, Pageable pageable) {
        Page<ChangeRequest> page = changeRequestRepository.findByFilters(
                status, requestor, entityType, changeType, fromDate, pageable);
        
        List<ChangeRequestDto> dtos = page.getContent().stream()
                .map(changeRequestMapper::toDto)
                .collect(Collectors.toList());
        
        return new PagedResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }
    
    public PagedResponse<ChangeRequestDto> findByRequestor(String requestor, Pageable pageable) {
        Page<ChangeRequest> page = changeRequestRepository.findByRequestor(requestor, pageable);
        
        List<ChangeRequestDto> dtos = page.getContent().stream()
                .map(changeRequestMapper::toDto)
                .collect(Collectors.toList());
        
        return new PagedResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }
    
    public List<ChangeRequestDto> findPending() {
        Page<ChangeRequest> page = changeRequestRepository.findByStatus("PENDING", Pageable.unpaged());
        return page.getContent().stream()
                .map(changeRequestMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public List<ChangeRequestDto> findHighPriorityPending() {
        return changeRequestRepository.findHighPriorityPending().stream()
                .map(changeRequestMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ChangeRequestDto create(ChangeRequestDto dto) {
        ChangeRequest entity = changeRequestMapper.toEntity(dto);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setStatus("PENDING");
        
        // Validate that the entity exists if entityId is provided
        if (entity.getEntityId() != null) {
            validateEntityExists(entity.getEntityType(), entity.getEntityId());
        }
        
        ChangeRequest saved = changeRequestRepository.save(entity);
        
        // Publish change request created event
        publishChangeRequestEvent(saved, "CHANGE_REQUEST_CREATED");
        
        logger.info("Created change request {} for {} {}", saved.getId(), saved.getEntityType(), saved.getEntityId());
        
        return changeRequestMapper.toDto(saved);
    }
    
    @Transactional
    public Optional<ChangeRequestDto> update(UUID id, ChangeRequestDto dto) {
        return changeRequestRepository.findById(id)
                .filter(existing -> "PENDING".equals(existing.getStatus()))
                .map(existing -> {
                    changeRequestMapper.updateEntityFromDto(dto, existing);
                    existing.setUpdatedAt(LocalDateTime.now());
                    
                    ChangeRequest saved = changeRequestRepository.save(existing);
                    publishChangeRequestEvent(saved, "CHANGE_REQUEST_UPDATED");
                    
                    logger.info("Updated change request {}", saved.getId());
                    
                    return changeRequestMapper.toDto(saved);
                });
    }
    
    @Transactional
    public Optional<ChangeRequestDto> approve(UUID id, String comments) {
        return changeRequestRepository.findById(id)
                .filter(existing -> "PENDING".equals(existing.getStatus()))
                .map(existing -> {
                    existing.setStatus("APPROVED");
                    existing.setApprovedAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    // TODO: Set approver from security context
                    existing.setApprover("SYSTEM"); // Placeholder
                    
                    if (comments != null) {
                        // Add comments to metadata or a comments field if needed
                        String currentMetadata = existing.getMetadata() != null ? existing.getMetadata() : "";
                        existing.setMetadata(currentMetadata + " | Approval comments: " + comments);
                    }
                    
                    ChangeRequest saved = changeRequestRepository.save(existing);
                    publishChangeRequestEvent(saved, "CHANGE_REQUEST_APPROVED");
                    
                    logger.info("Approved change request {}", saved.getId());
                    
                    return changeRequestMapper.toDto(saved);
                });
    }
    
    @Transactional
    public Optional<ChangeRequestDto> reject(UUID id, String reason) {
        return changeRequestRepository.findById(id)
                .filter(existing -> "PENDING".equals(existing.getStatus()))
                .map(existing -> {
                    existing.setStatus("REJECTED");
                    existing.setRejectionReason(reason);
                    existing.setUpdatedAt(LocalDateTime.now());
                    // TODO: Set approver from security context
                    existing.setApprover("SYSTEM"); // Placeholder
                    
                    ChangeRequest saved = changeRequestRepository.save(existing);
                    publishChangeRequestEvent(saved, "CHANGE_REQUEST_REJECTED");
                    
                    logger.info("Rejected change request {}: {}", saved.getId(), reason);
                    
                    return changeRequestMapper.toDto(saved);
                });
    }
    
    @Transactional
    public Optional<ChangeRequestDto> cancel(UUID id) {
        return changeRequestRepository.findById(id)
                .filter(existing -> "PENDING".equals(existing.getStatus()) || "APPROVED".equals(existing.getStatus()))
                .map(existing -> {
                    existing.setStatus("CANCELLED");
                    existing.setUpdatedAt(LocalDateTime.now());
                    
                    ChangeRequest saved = changeRequestRepository.save(existing);
                    publishChangeRequestEvent(saved, "CHANGE_REQUEST_CANCELLED");
                    
                    logger.info("Cancelled change request {}", saved.getId());
                    
                    return changeRequestMapper.toDto(saved);
                });
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalRequests", changeRequestRepository.count());
        stats.put("pendingRequests", changeRequestRepository.countPending());
        
        // TODO: Add more detailed statistics
        stats.put("requestsByStatus", Map.of(
                "pending", changeRequestRepository.countPending(),
                "approved", 0L, // TODO: Implement these counts
                "rejected", 0L,
                "applied", 0L,
                "cancelled", 0L
        ));
        
        return stats;
    }
    
    private void validateEntityExists(String entityType, UUID entityId) {
        // TODO: Implement validation based on entity type
        // This would check if the referenced entity (country, port, etc.) exists
        logger.debug("Validating entity exists: {} {}", entityType, entityId);
    }
    
    private void publishChangeRequestEvent(ChangeRequest changeRequest, String eventType) {
        try {
            String payload = String.format("""
                {
                    "id": "%s",
                    "changeType": "%s",
                    "entityType": "%s",
                    "entityId": "%s",
                    "status": "%s",
                    "requestor": "%s",
                    "createdAt": "%s"
                }
                """, 
                changeRequest.getId(),
                changeRequest.getChangeType(),
                changeRequest.getEntityType(),
                changeRequest.getEntityId(),
                changeRequest.getStatus(),
                changeRequest.getRequestor(),
                changeRequest.getCreatedAt()
            );
            
            outboxPublisher.createEvent(
                    changeRequest.getId().toString(),
                    "CHANGE_REQUEST",
                    eventType,
                    payload
            );
        } catch (Exception e) {
            logger.error("Failed to publish event {} for change request {}", eventType, changeRequest.getId(), e);
            // Don't fail the transaction for event publishing errors
        }
    }
}