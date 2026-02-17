package gov.dhs.cbp.reference.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.dto.CountryDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.exception.BusinessException;
import gov.dhs.cbp.reference.api.exception.ResourceNotFoundException;
import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import gov.dhs.cbp.reference.core.repository.ChangeRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Application service that orchestrates the change request workflow.
 * This service acts as a higher-level orchestration layer that coordinates
 * between different services, handles complex business logic and validations,
 * supports transaction management and rollback, and provides audit trail.
 */
@Service
@Transactional
public class ChangeRequestApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ChangeRequestApplicationService.class);

    private final CountryChangeRequestService countryChangeRequestService;
    private final CountryService countryService;
    private final ChangeRequestRepository changeRequestRepository;
    private final ObjectMapper objectMapper;

    // Status constants
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_APPLIED = "APPLIED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    public ChangeRequestApplicationService(
            CountryChangeRequestService countryChangeRequestService,
            CountryService countryService,
            ChangeRequestRepository changeRequestRepository,
            ObjectMapper objectMapper) {
        this.countryChangeRequestService = countryChangeRequestService;
        this.countryService = countryService;
        this.changeRequestRepository = changeRequestRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates a change request before approval.
     * Performs comprehensive validation including business rules and data integrity.
     *
     * @param changeRequestId The ID of the change request to validate
     * @return true if validation passes, false if request is not in valid state for validation
     * @throws ResourceNotFoundException if the change request is not found
     * @throws BusinessException if validation fails
     */
    @Transactional(readOnly = true)
    public boolean validateChangeRequest(UUID changeRequestId) {
        logger.debug("Validating change request: {}", changeRequestId);

        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Change request not found with id: " + changeRequestId));

        // Only validate pending requests
        if (!STATUS_PENDING.equals(changeRequest.getStatus())) {
            logger.warn("Change request {} is not in PENDING status: {}", changeRequestId, changeRequest.getStatus());
            return false;
        }

        try {
            // Validate based on data type
            switch (changeRequest.getDataType()) {
                case "COUNTRY":
                    validateCountryChangeRequest(changeRequest);
                    break;
                default:
                    throw new BusinessException(
                        "Unsupported data type for validation: " + changeRequest.getDataType(),
                        HttpStatus.BAD_REQUEST
                    );
            }

            logger.info("Change request {} validation passed", changeRequestId);
            return true;

        } catch (JsonProcessingException e) {
            throw new BusinessException(
                "Failed to parse change request data: " + e.getMessage(),
                HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * Orchestrates the approval flow for a change request.
     * Validates the request before approval and handles the approval process.
     *
     * @param changeRequestId The ID of the change request to approve
     * @param approverUserId The ID of the user approving the request
     * @param comments Optional approval comments
     * @return The approved change request
     * @throws ResourceNotFoundException if the change request is not found
     * @throws BusinessException if the request cannot be approved
     */
    public ChangeRequest processApproval(UUID changeRequestId, String approverUserId, String comments) {
        logger.info("Processing approval for change request: {} by user: {}", changeRequestId, approverUserId);

        // Validate the change request first
        boolean isValid = validateChangeRequest(changeRequestId);
        if (!isValid) {
            throw new BusinessException(
                "Change request validation failed or request is not in pending status",
                HttpStatus.BAD_REQUEST
            );
        }

        // Proceed with approval
        ChangeRequest approvedRequest = countryChangeRequestService.approveChangeRequest(
            changeRequestId, approverUserId, comments);

        logger.info("Change request {} approved successfully by user: {}", changeRequestId, approverUserId);

        // TODO: Publish approval event (Issue #5)
        // eventPublisher.publishApprovalEvent(approvedRequest);

        return approvedRequest;
    }

    /**
     * Orchestrates the approval flow for a change request without comments.
     *
     * @param changeRequestId The ID of the change request to approve
     * @param approverUserId The ID of the user approving the request
     * @return The approved change request
     */
    public ChangeRequest processApproval(UUID changeRequestId, String approverUserId) {
        return processApproval(changeRequestId, approverUserId, null);
    }

    /**
     * Handles rejection of a change request.
     *
     * @param changeRequestId The ID of the change request to reject
     * @param rejectorUserId The ID of the user rejecting the request
     * @param reason The reason for rejection
     * @return The rejected change request
     * @throws ResourceNotFoundException if the change request is not found
     * @throws BusinessException if the request cannot be rejected
     */
    public ChangeRequest processRejection(UUID changeRequestId, String rejectorUserId, String reason) {
        logger.info("Processing rejection for change request: {} by user: {}", changeRequestId, rejectorUserId);

        ChangeRequest rejectedRequest = countryChangeRequestService.rejectChangeRequest(
            changeRequestId, rejectorUserId, reason);

        logger.info("Change request {} rejected by user: {} with reason: {}",
            changeRequestId, rejectorUserId, reason);

        // TODO: Publish rejection event (Issue #5)
        // eventPublisher.publishRejectionEvent(rejectedRequest);

        return rejectedRequest;
    }

    /**
     * Applies approved changes with rollback capability.
     * Executes the approved change request and handles any failures with proper rollback.
     *
     * @param changeRequestId The ID of the change request to execute
     * @return The applied change request
     * @throws ResourceNotFoundException if the change request is not found
     * @throws BusinessException if the request cannot be executed
     */
    public ChangeRequest executeChangeRequest(UUID changeRequestId) {
        logger.info("Executing change request: {}", changeRequestId);

        try {
            ChangeRequest appliedRequest = countryChangeRequestService.applyChangeRequest(changeRequestId);

            logger.info("Change request {} executed successfully", changeRequestId);

            // TODO: Publish execution event (Issue #5)
            // eventPublisher.publishExecutionEvent(appliedRequest);

            return appliedRequest;

        } catch (Exception e) {
            logger.error("Failed to execute change request {}: {}", changeRequestId, e.getMessage(), e);

            // In a full implementation, we would handle rollback here
            // For now, we re-throw the exception to maintain transaction boundaries
            throw e;
        }
    }

    /**
     * Schedules a change request for future execution.
     *
     * @param changeRequestId The ID of the change request to schedule
     * @param effectiveDate The date when the change should take effect
     * @return The updated change request
     * @throws ResourceNotFoundException if the change request is not found
     * @throws BusinessException if the effective date is invalid
     */
    public ChangeRequest scheduleChangeRequest(UUID changeRequestId, LocalDate effectiveDate) {
        logger.info("Scheduling change request: {} for effective date: {}", changeRequestId, effectiveDate);

        if (effectiveDate.isBefore(LocalDate.now())) {
            throw new BusinessException(
                "Effective date cannot be in the past",
                HttpStatus.BAD_REQUEST
            );
        }

        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Change request not found with id: " + changeRequestId));

        if (!STATUS_APPROVED.equals(changeRequest.getStatus())) {
            throw new BusinessException(
                "Only approved change requests can be scheduled",
                HttpStatus.BAD_REQUEST
            );
        }

        // Store the effective date in metadata
        try {
            Map<String, Object> metadata = new HashMap<>();
            if (changeRequest.getMetadata() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> existingMetadata = objectMapper.readValue(changeRequest.getMetadata(), Map.class);
                metadata = existingMetadata;
            }
            metadata.put("effectiveDate", effectiveDate.toString());
            metadata.put("scheduledAt", LocalDateTime.now().toString());

            changeRequest.setMetadata(objectMapper.writeValueAsString(metadata));

        } catch (JsonProcessingException e) {
            logger.warn("Failed to update metadata for scheduled change request {}: {}",
                changeRequestId, e.getMessage());
        }

        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);

        logger.info("Change request {} scheduled for {}", changeRequestId, effectiveDate);

        return savedRequest;
    }

    /**
     * Cancels a pending change request.
     *
     * @param changeRequestId The ID of the change request to cancel
     * @param userId The ID of the user cancelling the request
     * @param reason The reason for cancellation
     * @return The cancelled change request
     * @throws ResourceNotFoundException if the change request is not found
     * @throws BusinessException if the request cannot be cancelled
     */
    public ChangeRequest cancelChangeRequest(UUID changeRequestId, String userId, String reason) {
        logger.info("Cancelling change request: {} by user: {}", changeRequestId, userId);

        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Change request not found with id: " + changeRequestId));

        // Check if request can be cancelled
        if (STATUS_APPLIED.equals(changeRequest.getStatus())) {
            throw new BusinessException(
                "Cannot cancel a change request that has already been applied",
                HttpStatus.BAD_REQUEST
            );
        }

        if (STATUS_CANCELLED.equals(changeRequest.getStatus())) {
            throw new BusinessException(
                "Change request is already cancelled",
                HttpStatus.BAD_REQUEST
            );
        }

        // Update status and add cancellation details
        changeRequest.setStatus(STATUS_CANCELLED);
        changeRequest.setRejectionReason(reason); // Reuse rejection reason field for cancellation
        changeRequest.setRejectedBy(userId);
        changeRequest.setRejectedAt(LocalDateTime.now());

        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);

        logger.info("Change request {} cancelled by user: {} with reason: {}",
            changeRequestId, userId, reason);

        // TODO: Publish cancellation event (Issue #5)
        // eventPublisher.publishCancellationEvent(savedRequest);

        return savedRequest;
    }

    /**
     * Retrieves dashboard metrics for change requests.
     *
     * @return Map containing various metrics about change requests
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getChangeRequestMetrics() {
        logger.debug("Retrieving change request metrics");

        Map<String, Object> metrics = new HashMap<>();

        // Get counts by status
        long pendingCount = changeRequestRepository.countPending();
        long totalCount = changeRequestRepository.count();

        // Get approved and rejected counts using repository queries
        long approvedCount = changeRequestRepository.findByStatus(STATUS_APPROVED, PageRequest.of(0, 1))
            .getTotalElements();
        long rejectedCount = changeRequestRepository.findByStatus(STATUS_REJECTED, PageRequest.of(0, 1))
            .getTotalElements();

        metrics.put("pendingCount", pendingCount);
        metrics.put("approvedCount", approvedCount);
        metrics.put("rejectedCount", rejectedCount);
        metrics.put("totalCount", totalCount);

        // Calculate additional metrics
        metrics.put("approvalRate", totalCount > 0 ? (double) approvedCount / totalCount * 100 : 0.0);
        metrics.put("rejectionRate", totalCount > 0 ? (double) rejectedCount / totalCount * 100 : 0.0);

        logger.debug("Retrieved metrics: {}", metrics);

        return metrics;
    }

    /**
     * Retrieves change requests submitted by a specific user.
     *
     * @param userId The ID of the user
     * @param pageable Pagination parameters
     * @return Paged response of user's change requests
     */
    @Transactional(readOnly = true)
    public PagedResponse<ChangeRequest> getMyChangeRequests(String userId, Pageable pageable) {
        logger.debug("Retrieving change requests for user: {} with pagination: {}", userId, pageable);

        Page<ChangeRequest> page = changeRequestRepository.findByRequestor(userId, pageable);

        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements()
        );
    }

    /**
     * Retrieves change requests awaiting approval by a specific user.
     * Currently returns all pending requests, but could be enhanced with
     * role-based filtering or assignment logic.
     *
     * @param userId The ID of the approver user
     * @param pageable Pagination parameters
     * @return Paged response of change requests awaiting approval
     */
    @Transactional(readOnly = true)
    public PagedResponse<ChangeRequest> getChangeRequestsForApproval(String userId, Pageable pageable) {
        logger.debug("Retrieving change requests for approval by user: {} with pagination: {}", userId, pageable);

        // For now, return all pending requests
        // In a full implementation, this would filter based on user roles/permissions
        Page<ChangeRequest> page = changeRequestRepository.findByStatus(STATUS_PENDING, pageable);

        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements()
        );
    }

    /**
     * Approves multiple change requests in bulk.
     * Only processes valid requests and skips invalid ones.
     *
     * @param changeRequestIds List of change request IDs to approve
     * @param userId The ID of the approving user
     * @return List of successfully approved change requests
     */
    public List<ChangeRequest> bulkApprove(List<UUID> changeRequestIds, String userId) {
        logger.info("Processing bulk approval for {} requests by user: {}",
            changeRequestIds.size(), userId);

        if (changeRequestIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChangeRequest> approvedRequests = new ArrayList<>();

        for (UUID requestId : changeRequestIds) {
            try {
                // Validate and approve each request
                if (validateChangeRequest(requestId)) {
                    ChangeRequest approved = countryChangeRequestService.approveChangeRequest(
                        requestId, userId, null);
                    approvedRequests.add(approved);
                    logger.debug("Bulk approved change request: {}", requestId);
                } else {
                    logger.warn("Skipped change request {} - not eligible for approval", requestId);
                }
            } catch (Exception e) {
                logger.error("Failed to approve change request {} in bulk operation: {}",
                    requestId, e.getMessage());
                // Continue with other requests rather than failing the entire operation
            }
        }

        logger.info("Bulk approval completed. Approved {} out of {} requests",
            approvedRequests.size(), changeRequestIds.size());

        // TODO: Publish bulk approval event (Issue #5)
        // eventPublisher.publishBulkApprovalEvent(approvedRequests, userId);

        return approvedRequests;
    }

    // Private helper methods

    private void validateCountryChangeRequest(ChangeRequest changeRequest) throws JsonProcessingException {
        if (changeRequest.getProposedChanges() != null) {
            CountryDto countryDto = objectMapper.readValue(
                changeRequest.getProposedChanges(), CountryDto.class);
            countryService.validateCountryData(countryDto);
        } else if ("CREATE".equals(changeRequest.getOperationType())) {
            throw new BusinessException(
                "Proposed changes are required for CREATE operations",
                HttpStatus.BAD_REQUEST
            );
        }

        // Additional validation for UPDATE operations
        if ("UPDATE".equals(changeRequest.getOperationType()) && changeRequest.getCurrentValues() == null) {
            throw new BusinessException(
                "Current values are required for UPDATE operations",
                HttpStatus.BAD_REQUEST
            );
        }
    }
}