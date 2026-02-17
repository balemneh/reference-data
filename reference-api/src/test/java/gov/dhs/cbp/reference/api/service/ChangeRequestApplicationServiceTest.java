package gov.dhs.cbp.reference.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.dto.CountryDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.exception.BusinessException;
import gov.dhs.cbp.reference.api.exception.ResourceNotFoundException;
import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import gov.dhs.cbp.reference.core.repository.ChangeRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for ChangeRequestApplicationService using TDD approach.
 * Tests the orchestration layer for change request workflow.
 */
@ExtendWith(MockitoExtension.class)
class ChangeRequestApplicationServiceTest {

    @Mock
    private CountryChangeRequestService countryChangeRequestService;

    @Mock
    private CountryService countryService;

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ChangeRequestApplicationService applicationService;

    private UUID changeRequestId;
    private String userId;
    private ChangeRequest pendingChangeRequest;
    private ChangeRequest approvedChangeRequest;
    private CountryDto countryDto;

    @BeforeEach
    void setUp() {
        changeRequestId = UUID.randomUUID();
        userId = "user123";

        // Create test data
        pendingChangeRequest = createTestChangeRequest("PENDING");
        approvedChangeRequest = createTestChangeRequest("APPROVED");
        countryDto = createTestCountryDto();
    }

    @Test
    void validateChangeRequest_WithValidPendingRequest_ShouldReturnTrue() {
        // Given
        when(changeRequestRepository.findById(changeRequestId))
            .thenReturn(Optional.of(pendingChangeRequest));
        try {
            when(objectMapper.readValue(anyString(), eq(CountryDto.class)))
                .thenReturn(countryDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // When
        boolean result = applicationService.validateChangeRequest(changeRequestId);

        // Then
        assertThat(result).isTrue();
        verify(countryService).validateCountryData(countryDto);
    }

    @Test
    void validateChangeRequest_WithNonExistentRequest_ShouldThrowException() {
        // Given
        when(changeRequestRepository.findById(changeRequestId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> applicationService.validateChangeRequest(changeRequestId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Change request not found");
    }

    @Test
    void validateChangeRequest_WithNonPendingStatus_ShouldReturnFalse() {
        // Given
        ChangeRequest approvedRequest = createTestChangeRequest("APPROVED");
        when(changeRequestRepository.findById(changeRequestId))
            .thenReturn(Optional.of(approvedRequest));

        // When
        boolean result = applicationService.validateChangeRequest(changeRequestId);

        // Then
        assertThat(result).isFalse();
        verify(countryService, never()).validateCountryData(any());
    }

    @Test
    void validateChangeRequest_WithInvalidCountryData_ShouldThrowException() {
        // Given
        when(changeRequestRepository.findById(changeRequestId))
            .thenReturn(Optional.of(pendingChangeRequest));
        try {
            when(objectMapper.readValue(anyString(), eq(CountryDto.class)))
                .thenReturn(countryDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        doThrow(new BusinessException("Invalid country data", null))
            .when(countryService).validateCountryData(countryDto);

        // When & Then
        assertThatThrownBy(() -> applicationService.validateChangeRequest(changeRequestId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Invalid country data");
    }

    @Test
    void processApproval_WithValidRequest_ShouldApproveAndReturnRequest() {
        // Given
        String approverUserId = "approver123";
        String comments = "Approved for implementation";

        when(changeRequestRepository.findById(changeRequestId))
            .thenReturn(Optional.of(pendingChangeRequest));
        try {
            when(objectMapper.readValue(anyString(), eq(CountryDto.class)))
                .thenReturn(countryDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        when(countryChangeRequestService.approveChangeRequest(changeRequestId, approverUserId, comments))
            .thenReturn(approvedChangeRequest);

        // When
        ChangeRequest result = applicationService.processApproval(changeRequestId, approverUserId, comments);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        verify(countryChangeRequestService).approveChangeRequest(changeRequestId, approverUserId, comments);
    }

    @Test
    void processApproval_WithInvalidRequest_ShouldThrowException() {
        // Given
        String approverUserId = "approver123";
        when(changeRequestRepository.findById(changeRequestId))
            .thenReturn(Optional.of(pendingChangeRequest));
        try {
            when(objectMapper.readValue(anyString(), eq(CountryDto.class)))
                .thenReturn(countryDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        doThrow(new BusinessException("Validation failed", null))
            .when(countryService).validateCountryData(countryDto);

        // When & Then
        assertThatThrownBy(() -> applicationService.processApproval(changeRequestId, approverUserId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Validation failed");
    }

    @Test
    void processRejection_WithValidRequest_ShouldRejectAndReturnRequest() {
        // Given
        String rejectorUserId = "rejector123";
        String reason = "Insufficient business justification";
        ChangeRequest rejectedRequest = createTestChangeRequest("REJECTED");

        when(countryChangeRequestService.rejectChangeRequest(changeRequestId, rejectorUserId, reason))
            .thenReturn(rejectedRequest);

        // When
        ChangeRequest result = applicationService.processRejection(changeRequestId, rejectorUserId, reason);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("REJECTED");
        verify(countryChangeRequestService).rejectChangeRequest(changeRequestId, rejectorUserId, reason);
    }

    @Test
    void executeChangeRequest_WithApprovedRequest_ShouldExecuteAndReturnRequest() {
        // Given
        ChangeRequest appliedRequest = createTestChangeRequest("APPLIED");
        when(countryChangeRequestService.applyChangeRequest(changeRequestId))
            .thenReturn(appliedRequest);

        // When
        ChangeRequest result = applicationService.executeChangeRequest(changeRequestId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("APPLIED");
        verify(countryChangeRequestService).applyChangeRequest(changeRequestId);
    }

    @Test
    void executeChangeRequest_WithRollbackOnFailure_ShouldHandleException() {
        // Given
        when(countryChangeRequestService.applyChangeRequest(changeRequestId))
            .thenThrow(new BusinessException("Execution failed", null));

        // When & Then
        assertThatThrownBy(() -> applicationService.executeChangeRequest(changeRequestId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Execution failed");
    }

    @Test
    void scheduleChangeRequest_WithValidDate_ShouldUpdateScheduledDate() {
        // Given
        LocalDate effectiveDate = LocalDate.now().plusDays(7);
        when(changeRequestRepository.findById(changeRequestId))
            .thenReturn(Optional.of(approvedChangeRequest));
        when(changeRequestRepository.save(any(ChangeRequest.class)))
            .thenReturn(approvedChangeRequest);

        // When
        ChangeRequest result = applicationService.scheduleChangeRequest(changeRequestId, effectiveDate);

        // Then
        assertThat(result).isNotNull();
        verify(changeRequestRepository).save(any(ChangeRequest.class));
    }

    @Test
    void scheduleChangeRequest_WithPastDate_ShouldThrowException() {
        // Given
        LocalDate pastDate = LocalDate.now().minusDays(1);

        // When & Then
        assertThatThrownBy(() -> applicationService.scheduleChangeRequest(changeRequestId, pastDate))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Effective date cannot be in the past");

        // The service should fail fast on date validation before repository call
        verify(changeRequestRepository, never()).findById(any());
    }

    @Test
    void cancelChangeRequest_WithValidRequest_ShouldCancelAndReturnRequest() {
        // Given
        String reason = "Requirements changed";
        ChangeRequest cancelledRequest = createTestChangeRequest("CANCELLED");

        when(changeRequestRepository.findById(changeRequestId))
            .thenReturn(Optional.of(pendingChangeRequest));
        when(changeRequestRepository.save(any(ChangeRequest.class)))
            .thenReturn(cancelledRequest);

        // When
        ChangeRequest result = applicationService.cancelChangeRequest(changeRequestId, userId, reason);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(changeRequestRepository).save(any(ChangeRequest.class));
    }

    @Test
    void cancelChangeRequest_WithAppliedRequest_ShouldThrowException() {
        // Given
        ChangeRequest appliedRequest = createTestChangeRequest("APPLIED");
        when(changeRequestRepository.findById(changeRequestId))
            .thenReturn(Optional.of(appliedRequest));

        // When & Then
        assertThatThrownBy(() -> applicationService.cancelChangeRequest(changeRequestId, userId, "test"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Cannot cancel");
    }

    @Test
    void getChangeRequestMetrics_ShouldReturnCorrectCounts() {
        // Given
        when(changeRequestRepository.countPending()).thenReturn(5L);
        when(changeRequestRepository.count()).thenReturn(50L);

        // Set up additional mock calls for approved and rejected counts
        Page<ChangeRequest> approvedPage = new PageImpl<>(Collections.emptyList());
        Page<ChangeRequest> rejectedPage = new PageImpl<>(Collections.emptyList());
        approvedPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 1), 3L);
        rejectedPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 1), 2L);

        when(changeRequestRepository.findByStatus(eq("APPROVED"), any(Pageable.class)))
            .thenReturn(approvedPage);
        when(changeRequestRepository.findByStatus(eq("REJECTED"), any(Pageable.class)))
            .thenReturn(rejectedPage);

        // When
        Map<String, Object> metrics = applicationService.getChangeRequestMetrics();

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.get("pendingCount")).isEqualTo(5L);
        assertThat(metrics.get("approvedCount")).isEqualTo(3L);
        assertThat(metrics.get("rejectedCount")).isEqualTo(2L);
        assertThat(metrics.get("totalCount")).isEqualTo(50L);
    }

    @Test
    void getMyChangeRequests_WithValidUser_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<ChangeRequest> requests = Arrays.asList(pendingChangeRequest, approvedChangeRequest);
        Page<ChangeRequest> page = new PageImpl<>(requests, pageable, 2);

        when(changeRequestRepository.findByRequestor(userId, pageable))
            .thenReturn(page);

        // When
        PagedResponse<ChangeRequest> result = applicationService.getMyChangeRequests(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(changeRequestRepository).findByRequestor(userId, pageable);
    }

    @Test
    void getChangeRequestsForApproval_WithValidUser_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<ChangeRequest> requests = Arrays.asList(pendingChangeRequest);
        Page<ChangeRequest> page = new PageImpl<>(requests, pageable, 1);

        when(changeRequestRepository.findByStatus("PENDING", pageable))
            .thenReturn(page);

        // When
        PagedResponse<ChangeRequest> result = applicationService.getChangeRequestsForApproval(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(changeRequestRepository).findByStatus("PENDING", pageable);
    }

    @Test
    void bulkApprove_WithValidRequests_ShouldApproveAllRequests() {
        // Given
        UUID requestId1 = UUID.randomUUID();
        UUID requestId2 = UUID.randomUUID();
        List<UUID> requestIds = Arrays.asList(requestId1, requestId2);
        String approverUserId = "approver123";

        ChangeRequest request1 = createTestChangeRequest("PENDING");
        ChangeRequest request2 = createTestChangeRequest("PENDING");
        request1.setId(requestId1);
        request2.setId(requestId2);

        when(changeRequestRepository.findById(requestId1))
            .thenReturn(Optional.of(request1));
        when(changeRequestRepository.findById(requestId2))
            .thenReturn(Optional.of(request2));
        try {
            when(objectMapper.readValue(anyString(), eq(CountryDto.class)))
                .thenReturn(countryDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        when(countryChangeRequestService.approveChangeRequest(any(UUID.class), eq(approverUserId), any()))
            .thenReturn(approvedChangeRequest);

        // When
        List<ChangeRequest> results = applicationService.bulkApprove(requestIds, approverUserId);

        // Then
        assertThat(results).hasSize(2);
        verify(countryChangeRequestService, times(2))
            .approveChangeRequest(any(UUID.class), eq(approverUserId), any());
    }

    @Test
    void bulkApprove_WithMixedStatuses_ShouldOnlyApproveValidRequests() {
        // Given
        UUID validRequestId = UUID.randomUUID();
        UUID invalidRequestId = UUID.randomUUID();
        List<UUID> requestIds = Arrays.asList(validRequestId, invalidRequestId);
        String approverUserId = "approver123";

        ChangeRequest validRequest = createTestChangeRequest("PENDING");
        ChangeRequest invalidRequest = createTestChangeRequest("APPROVED");
        validRequest.setId(validRequestId);
        invalidRequest.setId(invalidRequestId);

        when(changeRequestRepository.findById(validRequestId))
            .thenReturn(Optional.of(validRequest));
        when(changeRequestRepository.findById(invalidRequestId))
            .thenReturn(Optional.of(invalidRequest));
        try {
            when(objectMapper.readValue(anyString(), eq(CountryDto.class)))
                .thenReturn(countryDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        when(countryChangeRequestService.approveChangeRequest(validRequestId, approverUserId, null))
            .thenReturn(approvedChangeRequest);

        // When
        List<ChangeRequest> results = applicationService.bulkApprove(requestIds, approverUserId);

        // Then
        assertThat(results).hasSize(1);
        verify(countryChangeRequestService, times(1))
            .approveChangeRequest(validRequestId, approverUserId, null);
    }

    @Test
    void bulkApprove_WithEmptyList_ShouldReturnEmptyList() {
        // Given
        List<UUID> emptyRequestIds = Collections.emptyList();
        String approverUserId = "approver123";

        // When
        List<ChangeRequest> results = applicationService.bulkApprove(emptyRequestIds, approverUserId);

        // Then
        assertThat(results).isEmpty();
        verify(countryChangeRequestService, never())
            .approveChangeRequest(any(), any(), any());
    }

    // Helper methods for test data creation

    private ChangeRequest createTestChangeRequest(String status) {
        ChangeRequest request = new ChangeRequest();
        request.setId(changeRequestId);
        request.setCrNumber("CR-2025-123456");
        request.setTitle("Test Change Request");
        request.setDescription("Test Description");
        request.setOperationType("CREATE");
        request.setDataType("COUNTRY");
        request.setStatus(status);
        request.setRequesterId(userId);
        request.setBusinessJustification("Test justification");
        request.setPriority("MEDIUM");
        request.setCreatedAt(LocalDateTime.now());
        request.setProposedChanges("{\"countryCode\":\"TEST\",\"countryName\":\"Test Country\"}");
        return request;
    }

    private CountryDto createTestCountryDto() {
        CountryDto dto = new CountryDto();
        dto.setId(UUID.randomUUID());
        dto.setCountryCode("TEST");
        dto.setCountryName("Test Country");
        dto.setCodeSystem("ISO3166-1");
        dto.setIso2Code("TE");
        dto.setIso3Code("TES");
        dto.setNumericCode("999");
        dto.setIsActive(true);
        dto.setValidFrom(LocalDate.now());
        dto.setRecordedAt(LocalDateTime.now());
        dto.setRecordedBy("system");
        return dto;
    }
}