package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, UUID> {
    
    @Query("SELECT cr FROM ChangeRequest cr WHERE cr.status = :status ORDER BY cr.createdAt DESC")
    Page<ChangeRequest> findByStatus(@Param("status") String status, Pageable pageable);
    
    @Query("SELECT cr FROM ChangeRequest cr WHERE cr.requesterId = :requestor ORDER BY cr.createdAt DESC")
    Page<ChangeRequest> findByRequestor(@Param("requestor") String requestor, Pageable pageable);
    
    @Query("SELECT cr FROM ChangeRequest cr WHERE cr.dataType = :entityType " +
           "ORDER BY cr.createdAt DESC")
    List<ChangeRequest> findByEntityType(@Param("entityType") String entityType);
    
    @Query("SELECT cr FROM ChangeRequest cr WHERE " +
           "(:status IS NULL OR cr.status = :status) AND " +
           "(:requestor IS NULL OR cr.requesterId = :requestor) AND " +
           "(:entityType IS NULL OR cr.dataType = :entityType) AND " +
           "(:changeType IS NULL OR cr.operationType = :changeType) AND " +
           "cr.createdAt >= :fromDate " +
           "ORDER BY cr.createdAt DESC")
    Page<ChangeRequest> findByFilters(@Param("status") String status,
                                     @Param("requestor") String requestor,
                                     @Param("entityType") String entityType,
                                     @Param("changeType") String changeType,
                                     @Param("fromDate") LocalDateTime fromDate,
                                     Pageable pageable);
    
    @Query("SELECT cr FROM ChangeRequest cr WHERE cr.status = 'APPROVED' " +
           "AND cr.submittedAt <= CURRENT_TIMESTAMP " +
           "ORDER BY cr.submittedAt")
    List<ChangeRequest> findReadyToApply();
    
    @Query("SELECT cr FROM ChangeRequest cr WHERE cr.status = 'PENDING' " +
           "AND cr.priority = 'HIGH' " +
           "ORDER BY cr.createdAt")
    List<ChangeRequest> findHighPriorityPending();
    
    @Query("SELECT cr FROM ChangeRequest cr WHERE cr.workflowInstanceId = :workflowId")
    Optional<ChangeRequest> findByWorkflowInstanceId(@Param("workflowId") String workflowId);
    
    @Query("SELECT COUNT(cr) FROM ChangeRequest cr WHERE cr.status = 'PENDING'")
    long countPending();
    
    @Query("SELECT COUNT(cr) FROM ChangeRequest cr WHERE cr.requesterId = :requestor " +
           "AND cr.status = 'PENDING'")
    long countPendingByRequestor(@Param("requestor") String requestor);
    
    @Query("SELECT cr FROM ChangeRequest cr WHERE cr.status IN ('PENDING', 'APPROVED') " +
           "AND cr.dataType = :entityType")
    List<ChangeRequest> findActiveChangeRequestsForEntity(@Param("entityType") String entityType);
    
    // External ticket ID no longer exists in entity
    // @Query("SELECT cr FROM ChangeRequest cr WHERE cr.externalTicketId = :ticketId")
    // Optional<ChangeRequest> findByExternalTicketId(@Param("ticketId") String ticketId);
}