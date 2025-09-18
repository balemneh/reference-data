package gov.dhs.cbp.reference.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.dto.CountryDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.exception.BusinessException;
import gov.dhs.cbp.reference.api.exception.ResourceNotFoundException;
import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import gov.dhs.cbp.reference.core.repository.ChangeRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing country change requests in the approval workflow.
 * Handles creation, approval, rejection, and application of country changes.
 */
@Service
@Transactional
public class CountryChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final CountryService countryService;
    private final ObjectMapper objectMapper;

    private static final String DATA_TYPE = "COUNTRY";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_APPLIED = "APPLIED";

    public CountryChangeRequestService(
            ChangeRequestRepository changeRequestRepository,
            CountryService countryService,
            ObjectMapper objectMapper) {
        this.changeRequestRepository = changeRequestRepository;
        this.countryService = countryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new change request for a country operation.
     *
     * @param countryDto The country data to be changed
     * @param operation The operation type (CREATE, UPDATE, DELETE)
     * @param userId The ID of the user creating the request
     * @param reason The business justification for the change
     * @return The created change request
     * @throws IllegalArgumentException if required parameters are missing
     * @throws BusinessException if the operation is invalid or serialization fails
     */
    public ChangeRequest createChangeRequest(CountryDto countryDto, String operation, String userId, String reason) {
        validateChangeRequestData(countryDto, operation, userId);

        ChangeRequest changeRequest = new ChangeRequest();
        changeRequest.setCrNumber(generateCrNumber());
        changeRequest.setOperationType(operation.toUpperCase());
        changeRequest.setDataType(DATA_TYPE);
        changeRequest.setRequesterId(userId);
        changeRequest.setBusinessJustification(reason);
        changeRequest.setStatus(STATUS_PENDING);
        changeRequest.setPriority("MEDIUM");

        try {
            switch (operation.toUpperCase()) {
                case "CREATE":
                    setupCreateRequest(changeRequest, countryDto);
                    break;
                case "UPDATE":
                    setupUpdateRequest(changeRequest, countryDto);
                    break;
                case "DELETE":
                    setupDeleteRequest(changeRequest, countryDto);
                    break;
                default:
                    throw new BusinessException(
                        "Invalid operation type: " + operation + ". Supported operations: CREATE, UPDATE, DELETE",
                        HttpStatus.BAD_REQUEST
                    );
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                "Failed to serialize country data: " + e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        return changeRequestRepository.save(changeRequest);
    }

    /**
     * Retrieves a change request by its ID.
     *
     * @param id The change request ID
     * @return Optional containing the change request if found
     */
    @Transactional(readOnly = true)
    public Optional<ChangeRequest> getChangeRequestById(UUID id) {
        return changeRequestRepository.findById(id);
    }

    /**
     * Retrieves change requests by status with pagination.
     *
     * @param status The status to filter by
     * @param pageable Pagination parameters
     * @return Paged response of change requests
     */
    @Transactional(readOnly = true)
    public PagedResponse<ChangeRequest> getChangeRequestsByStatus(String status, Pageable pageable) {
        Page<ChangeRequest> page = changeRequestRepository.findByStatus(status, pageable);
        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements()
        );
    }

    /**
     * Retrieves pending change requests with pagination.
     *
     * @param pageable Pagination parameters
     * @return Paged response of pending change requests
     */
    @Transactional(readOnly = true)
    public PagedResponse<ChangeRequest> getPendingChangeRequests(Pageable pageable) {
        return getChangeRequestsByStatus(STATUS_PENDING, pageable);
    }

    /**
     * Approves a change request.
     *
     * @param id The change request ID
     * @param approverUserId The ID of the approving user
     * @param comments Optional approval comments
     * @return The approved change request
     * @throws ResourceNotFoundException if the change request is not found
     * @throws BusinessException if the request cannot be approved
     */
    public ChangeRequest approveChangeRequest(UUID id, String approverUserId, String comments) {
        ChangeRequest changeRequest = getChangeRequestById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Change request not found with id: " + id));

        if (!STATUS_PENDING.equals(changeRequest.getStatus())) {
            throw new BusinessException(
                "Change request is not in PENDING status. Current status: " + changeRequest.getStatus(),
                HttpStatus.BAD_REQUEST
            );
        }

        changeRequest.setStatus(STATUS_APPROVED);
        changeRequest.setApprovedBy(approverUserId);
        changeRequest.setApprovedAt(LocalDateTime.now());

        if (StringUtils.hasText(comments)) {
            try {
                String approvalData = objectMapper.writeValueAsString(
                    new ApprovalData(approverUserId, comments, LocalDateTime.now())
                );
                changeRequest.setApprovalData(approvalData);
            } catch (JsonProcessingException e) {
                // Log error but don't fail the approval
                changeRequest.setApprovalData("{\"comments\":\"" + comments + "\"}");
            }
        }

        return changeRequestRepository.save(changeRequest);
    }

    /**
     * Rejects a change request.
     *
     * @param id The change request ID
     * @param rejectorUserId The ID of the rejecting user
     * @param reason The reason for rejection
     * @return The rejected change request
     * @throws ResourceNotFoundException if the change request is not found
     * @throws BusinessException if the request cannot be rejected
     */
    public ChangeRequest rejectChangeRequest(UUID id, String rejectorUserId, String reason) {
        ChangeRequest changeRequest = getChangeRequestById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Change request not found with id: " + id));

        if (!STATUS_PENDING.equals(changeRequest.getStatus())) {
            throw new BusinessException(
                "Change request is not in PENDING status. Current status: " + changeRequest.getStatus(),
                HttpStatus.BAD_REQUEST
            );
        }

        changeRequest.setStatus(STATUS_REJECTED);
        changeRequest.setRejectedBy(rejectorUserId);
        changeRequest.setRejectedAt(LocalDateTime.now());
        changeRequest.setRejectionReason(reason);

        return changeRequestRepository.save(changeRequest);
    }

    /**
     * Applies an approved change request to the countries table.
     *
     * @param changeRequestId The change request ID
     * @return The applied change request
     * @throws ResourceNotFoundException if the change request is not found
     * @throws BusinessException if the request cannot be applied
     */
    public ChangeRequest applyChangeRequest(UUID changeRequestId) {
        ChangeRequest changeRequest = getChangeRequestById(changeRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Change request not found with id: " + changeRequestId));

        if (!STATUS_APPROVED.equals(changeRequest.getStatus())) {
            throw new BusinessException(
                "Change request is not in APPROVED status. Current status: " + changeRequest.getStatus(),
                HttpStatus.BAD_REQUEST
            );
        }

        try {
            switch (changeRequest.getOperationType()) {
                case "CREATE":
                    applyCreateOperation(changeRequest);
                    break;
                case "UPDATE":
                    applyUpdateOperation(changeRequest);
                    break;
                case "DELETE":
                    applyDeleteOperation(changeRequest);
                    break;
                default:
                    throw new BusinessException(
                        "Unsupported operation type: " + changeRequest.getOperationType(),
                        HttpStatus.INTERNAL_SERVER_ERROR
                    );
            }

            changeRequest.setStatus(STATUS_APPLIED);
            changeRequest.setImplementedAt(LocalDateTime.now());
            changeRequest.setImplementedBy("SYSTEM");

            return changeRequestRepository.save(changeRequest);

        } catch (JsonProcessingException e) {
            throw new BusinessException(
                "Failed to apply change request: " + e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Retrieves change request history for a specific entity.
     *
     * @param entityId The entity ID
     * @param pageable Pagination parameters
     * @return Paged response of change requests for the entity
     */
    @Transactional(readOnly = true)
    public PagedResponse<ChangeRequest> getChangeRequestHistory(UUID entityId, Pageable pageable) {
        // Search for change requests related to the country entity type
        // This is a simplified implementation - in a full implementation you might want
        // to add entity ID tracking to the change request
        LocalDateTime fromDate = LocalDateTime.now().minusYears(1); // Last year of history
        Page<ChangeRequest> page = changeRequestRepository.findByFilters(
            null, null, DATA_TYPE, null, fromDate, pageable
        );

        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements()
        );
    }

    // Private helper methods

    private void validateChangeRequestData(CountryDto countryDto, String operation, String userId) {
        if (countryDto == null) {
            throw new IllegalArgumentException("Country data is required");
        }
        if (!StringUtils.hasText(operation)) {
            throw new IllegalArgumentException("Operation type is required");
        }
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("User ID is required");
        }
    }

    private String generateCrNumber() {
        LocalDateTime now = LocalDateTime.now();
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String timestamp = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        return "CR-" + year + "-" + timestamp;
    }

    private void setupCreateRequest(ChangeRequest changeRequest, CountryDto countryDto) throws JsonProcessingException {
        changeRequest.setTitle("Create new country: " + countryDto.getCountryName());
        changeRequest.setDescription("Adding " + countryDto.getCountryName() + " country record");
        changeRequest.setProposedChanges(objectMapper.writeValueAsString(countryDto));
    }

    private void setupUpdateRequest(ChangeRequest changeRequest, CountryDto countryDto) throws JsonProcessingException {
        // Find existing country to store current values
        Optional<CountryDto> existingCountry = countryService.findById(countryDto.getId());
        if (existingCountry.isEmpty()) {
            throw new ResourceNotFoundException("Country not found with id: " + countryDto.getId());
        }

        changeRequest.setTitle("Update country: " + countryDto.getCountryName());
        changeRequest.setDescription("Updating " + countryDto.getCountryName() + " country record");
        changeRequest.setProposedChanges(objectMapper.writeValueAsString(countryDto));
        changeRequest.setCurrentValues(objectMapper.writeValueAsString(existingCountry.get()));
    }

    private void setupDeleteRequest(ChangeRequest changeRequest, CountryDto countryDto) throws JsonProcessingException {
        // Find existing country to store current values
        Optional<CountryDto> existingCountry = countryService.findById(countryDto.getId());
        if (existingCountry.isEmpty()) {
            throw new ResourceNotFoundException("Country not found with id: " + countryDto.getId());
        }

        changeRequest.setTitle("Delete country: " + countryDto.getCountryName());
        changeRequest.setDescription("Deleting " + countryDto.getCountryName() + " country record");
        changeRequest.setCurrentValues(objectMapper.writeValueAsString(existingCountry.get()));
    }

    private void applyCreateOperation(ChangeRequest changeRequest) throws JsonProcessingException {
        CountryDto countryDto = objectMapper.readValue(changeRequest.getProposedChanges(), CountryDto.class);

        // Validate the country data before creation
        countryService.validateCountryData(countryDto);

        // Create the country through CountryService
        CountryDto createdCountry = countryService.createCountryFromChangeRequest(
            countryDto,
            changeRequest.getImplementedBy() != null ? changeRequest.getImplementedBy() : "SYSTEM"
        );

        // Store the created country ID in metadata for reference
        try {
            String metadata = objectMapper.writeValueAsString(
                new OperationResult("CREATE", createdCountry.getId(), "Country created successfully")
            );
            changeRequest.setMetadata(metadata);
        } catch (JsonProcessingException e) {
            // Log error but don't fail the operation
        }
    }

    private void applyUpdateOperation(ChangeRequest changeRequest) throws JsonProcessingException {
        CountryDto countryDto = objectMapper.readValue(changeRequest.getProposedChanges(), CountryDto.class);

        // Validate the country data before update
        countryService.validateCountryData(countryDto);

        // Update the country through CountryService
        CountryDto updatedCountry = countryService.updateCountryFromChangeRequest(
            countryDto,
            changeRequest.getImplementedBy() != null ? changeRequest.getImplementedBy() : "SYSTEM"
        );

        // Store the operation result in metadata
        try {
            String metadata = objectMapper.writeValueAsString(
                new OperationResult("UPDATE", updatedCountry.getId(), "Country updated successfully")
            );
            changeRequest.setMetadata(metadata);
        } catch (JsonProcessingException e) {
            // Log error but don't fail the operation
        }
    }

    private void applyDeleteOperation(ChangeRequest changeRequest) throws JsonProcessingException {
        CountryDto countryDto = objectMapper.readValue(changeRequest.getCurrentValues(), CountryDto.class);

        // Deactivate the country through CountryService
        CountryDto deactivatedCountry = countryService.deactivateCountryFromChangeRequest(
            countryDto.getId(),
            changeRequest.getImplementedBy() != null ? changeRequest.getImplementedBy() : "SYSTEM"
        );

        // Store the operation result in metadata
        try {
            String metadata = objectMapper.writeValueAsString(
                new OperationResult("DELETE", deactivatedCountry.getId(), "Country deactivated successfully")
            );
            changeRequest.setMetadata(metadata);
        } catch (JsonProcessingException e) {
            // Log error but don't fail the operation
        }
    }

    // Helper class for approval data
    private static class ApprovalData {
        public final String approver;
        public final String comments;
        public final LocalDateTime approvedAt;

        public ApprovalData(String approver, String comments, LocalDateTime approvedAt) {
            this.approver = approver;
            this.comments = comments;
            this.approvedAt = approvedAt;
        }
    }

    // Helper class for operation results
    private static class OperationResult {
        public final String operation;
        public final UUID entityId;
        public final String message;
        public final LocalDateTime timestamp;

        public OperationResult(String operation, UUID entityId, String message) {
            this.operation = operation;
            this.entityId = entityId;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }
    }
}