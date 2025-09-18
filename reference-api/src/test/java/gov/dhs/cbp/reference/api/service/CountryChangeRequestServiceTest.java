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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountryChangeRequestServiceTest {

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private CountryService countryService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CountryChangeRequestService countryChangeRequestService;

    private CountryDto testCountryDto;
    private ChangeRequest testChangeRequest;
    private UUID testId;
    private String testUserId;
    private String testReason;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testUserId = "test-user-123";
        testReason = "Test business justification";

        testCountryDto = new CountryDto();
        testCountryDto.setId(testId);
        testCountryDto.setCountryCode("US");
        testCountryDto.setCountryName("United States");
        testCountryDto.setIso2Code("US");
        testCountryDto.setIso3Code("USA");
        testCountryDto.setNumericCode("840");
        testCountryDto.setCodeSystem("ISO3166-1");
        testCountryDto.setIsActive(true);
        testCountryDto.setValidFrom(LocalDate.now());
        testCountryDto.setRecordedBy(testUserId);

        testChangeRequest = new ChangeRequest();
        testChangeRequest.setId(testId);
        testChangeRequest.setCrNumber("CR-2025-001");
        testChangeRequest.setTitle("Create new country: United States");
        testChangeRequest.setDescription("Adding United States country record");
        testChangeRequest.setOperationType("CREATE");
        testChangeRequest.setDataType("COUNTRY");
        testChangeRequest.setStatus("PENDING");
        testChangeRequest.setRequesterId(testUserId);
        testChangeRequest.setBusinessJustification(testReason);
        testChangeRequest.setPriority("MEDIUM");
        testChangeRequest.setCreatedAt(LocalDateTime.now());
        testChangeRequest.setSubmittedAt(LocalDateTime.now());
    }

    @Test
    void createChangeRequest_WithValidCreateRequest_ShouldCreateSuccessfully() throws JsonProcessingException {
        // Given
        String expectedJson = "{\"countryCode\":\"US\",\"countryName\":\"United States\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);
        when(changeRequestRepository.save(any(ChangeRequest.class))).thenReturn(testChangeRequest);

        // When
        ChangeRequest result = countryChangeRequestService.createChangeRequest(
                testCountryDto, "CREATE", testUserId, testReason);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOperationType()).isEqualTo("CREATE");
        assertThat(result.getDataType()).isEqualTo("COUNTRY");
        assertThat(result.getRequesterId()).isEqualTo(testUserId);
        assertThat(result.getBusinessJustification()).isEqualTo(testReason);
        assertThat(result.getStatus()).isEqualTo("PENDING");

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        ChangeRequest saved = captor.getValue();
        assertThat(saved.getProposedChanges()).isEqualTo(expectedJson);
        assertThat(saved.getCrNumber()).startsWith("CR-");
        assertThat(saved.getTitle()).contains("Create new country: United States");
    }

    @Test
    void createChangeRequest_WithValidUpdateRequest_ShouldCreateSuccessfully() throws JsonProcessingException {
        // Given
        CountryDto existingCountry = new CountryDto();
        existingCountry.setId(testId);
        existingCountry.setCountryName("Old Name");

        String proposedJson = "{\"countryName\":\"United States\"}";
        String currentJson = "{\"countryName\":\"Old Name\"}";

        when(countryService.findById(testId)).thenReturn(Optional.of(existingCountry));
        when(objectMapper.writeValueAsString(testCountryDto)).thenReturn(proposedJson);
        when(objectMapper.writeValueAsString(existingCountry)).thenReturn(currentJson);
        when(changeRequestRepository.save(any(ChangeRequest.class))).thenReturn(testChangeRequest);

        // When
        ChangeRequest result = countryChangeRequestService.createChangeRequest(
                testCountryDto, "UPDATE", testUserId, testReason);

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        ChangeRequest saved = captor.getValue();
        assertThat(saved.getOperationType()).isEqualTo("UPDATE");
        assertThat(saved.getProposedChanges()).isEqualTo(proposedJson);
        assertThat(saved.getCurrentValues()).isEqualTo(currentJson);
        assertThat(saved.getTitle()).contains("Update country: United States");
    }

    @Test
    void createChangeRequest_WithUpdateRequestForNonExistentCountry_ShouldThrowException() {
        // Given
        when(countryService.findById(testId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.createChangeRequest(
                testCountryDto, "UPDATE", testUserId, testReason))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Country not found with id:");
    }

    @Test
    void createChangeRequest_WithDeleteRequest_ShouldCreateSuccessfully() throws JsonProcessingException {
        // Given
        CountryDto existingCountry = new CountryDto();
        existingCountry.setId(testId);
        existingCountry.setCountryName("United States");

        String currentJson = "{\"countryName\":\"United States\"}";

        when(countryService.findById(testId)).thenReturn(Optional.of(existingCountry));
        when(objectMapper.writeValueAsString(existingCountry)).thenReturn(currentJson);
        when(changeRequestRepository.save(any(ChangeRequest.class))).thenReturn(testChangeRequest);

        // When
        ChangeRequest result = countryChangeRequestService.createChangeRequest(
                testCountryDto, "DELETE", testUserId, testReason);

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        ChangeRequest saved = captor.getValue();
        assertThat(saved.getOperationType()).isEqualTo("DELETE");
        assertThat(saved.getCurrentValues()).isEqualTo(currentJson);
        assertThat(saved.getTitle()).contains("Delete country: United States");
    }

    @Test
    void createChangeRequest_WithInvalidOperation_ShouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.createChangeRequest(
                testCountryDto, "INVALID", testUserId, testReason))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid operation type: INVALID");
    }

    @Test
    void createChangeRequest_WithNullCountryDto_ShouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.createChangeRequest(
                null, "CREATE", testUserId, testReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country data is required");
    }

    @Test
    void createChangeRequest_WithNullUserId_ShouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.createChangeRequest(
                testCountryDto, "CREATE", null, testReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID is required");
    }

    @Test
    void getChangeRequestById_WithExistingId_ShouldReturnChangeRequest() {
        // Given
        when(changeRequestRepository.findById(testId)).thenReturn(Optional.of(testChangeRequest));

        // When
        Optional<ChangeRequest> result = countryChangeRequestService.getChangeRequestById(testId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(testId);
        verify(changeRequestRepository).findById(testId);
    }

    @Test
    void getChangeRequestById_WithNonExistentId_ShouldReturnEmpty() {
        // Given
        when(changeRequestRepository.findById(testId)).thenReturn(Optional.empty());

        // When
        Optional<ChangeRequest> result = countryChangeRequestService.getChangeRequestById(testId);

        // Then
        assertThat(result).isEmpty();
        verify(changeRequestRepository).findById(testId);
    }

    @Test
    void getChangeRequestsByStatus_ShouldReturnPagedResults() {
        // Given
        List<ChangeRequest> changeRequests = Arrays.asList(testChangeRequest);
        Page<ChangeRequest> page = new PageImpl<>(changeRequests, PageRequest.of(0, 10), 1);
        when(changeRequestRepository.findByStatus("PENDING", PageRequest.of(0, 10))).thenReturn(page);

        // When
        PagedResponse<ChangeRequest> result = countryChangeRequestService.getChangeRequestsByStatus(
                "PENDING", PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(testId);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(changeRequestRepository).findByStatus("PENDING", PageRequest.of(0, 10));
    }

    @Test
    void getPendingChangeRequests_ShouldReturnPendingRequests() {
        // Given
        List<ChangeRequest> changeRequests = Arrays.asList(testChangeRequest);
        Page<ChangeRequest> page = new PageImpl<>(changeRequests, PageRequest.of(0, 10), 1);
        when(changeRequestRepository.findByStatus("PENDING", PageRequest.of(0, 10))).thenReturn(page);

        // When
        PagedResponse<ChangeRequest> result = countryChangeRequestService.getPendingChangeRequests(
                PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("PENDING");
        verify(changeRequestRepository).findByStatus("PENDING", PageRequest.of(0, 10));
    }

    @Test
    void approveChangeRequest_WithValidRequest_ShouldApproveSuccessfully() {
        // Given
        testChangeRequest.setStatus("PENDING");
        when(changeRequestRepository.findById(testId)).thenReturn(Optional.of(testChangeRequest));
        when(changeRequestRepository.save(any(ChangeRequest.class))).thenReturn(testChangeRequest);

        String approverUserId = "approver-123";
        String comments = "Approval comments";

        // When
        ChangeRequest result = countryChangeRequestService.approveChangeRequest(testId, approverUserId, comments);

        // Then
        assertThat(result).isNotNull();
        verify(changeRequestRepository).findById(testId);

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        ChangeRequest approved = captor.getValue();

        assertThat(approved.getStatus()).isEqualTo("APPROVED");
        assertThat(approved.getApprovedBy()).isEqualTo(approverUserId);
        assertThat(approved.getApprovedAt()).isNotNull();
        if (approved.getApprovalData() != null) {
            assertThat(approved.getApprovalData()).contains(comments);
        }
    }

    @Test
    void approveChangeRequest_WithNonExistentRequest_ShouldThrowException() {
        // Given
        when(changeRequestRepository.findById(testId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.approveChangeRequest(testId, "approver-123", "comments"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Change request not found with id:");
    }

    @Test
    void approveChangeRequest_WithAlreadyApprovedRequest_ShouldThrowException() {
        // Given
        testChangeRequest.setStatus("APPROVED");
        when(changeRequestRepository.findById(testId)).thenReturn(Optional.of(testChangeRequest));

        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.approveChangeRequest(testId, "approver-123", "comments"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Change request is not in PENDING status");
    }

    @Test
    void rejectChangeRequest_WithValidRequest_ShouldRejectSuccessfully() {
        // Given
        testChangeRequest.setStatus("PENDING");
        when(changeRequestRepository.findById(testId)).thenReturn(Optional.of(testChangeRequest));
        when(changeRequestRepository.save(any(ChangeRequest.class))).thenReturn(testChangeRequest);

        String rejectorUserId = "rejector-123";
        String reason = "Rejection reason";

        // When
        ChangeRequest result = countryChangeRequestService.rejectChangeRequest(testId, rejectorUserId, reason);

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        ChangeRequest rejected = captor.getValue();

        assertThat(rejected.getStatus()).isEqualTo("REJECTED");
        assertThat(rejected.getRejectedBy()).isEqualTo(rejectorUserId);
        assertThat(rejected.getRejectedAt()).isNotNull();
        assertThat(rejected.getRejectionReason()).isEqualTo(reason);
    }

    @Test
    void rejectChangeRequest_WithNonExistentRequest_ShouldThrowException() {
        // Given
        when(changeRequestRepository.findById(testId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.rejectChangeRequest(testId, "rejector-123", "reason"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Change request not found with id:");
    }

    @Test
    void rejectChangeRequest_WithAlreadyRejectedRequest_ShouldThrowException() {
        // Given
        testChangeRequest.setStatus("REJECTED");
        when(changeRequestRepository.findById(testId)).thenReturn(Optional.of(testChangeRequest));

        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.rejectChangeRequest(testId, "rejector-123", "reason"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Change request is not in PENDING status");
    }

    @Test
    void applyChangeRequest_WithApprovedCreateRequest_ShouldApplySuccessfully() throws JsonProcessingException {
        // Given
        testChangeRequest.setStatus("APPROVED");
        testChangeRequest.setOperationType("CREATE");
        testChangeRequest.setProposedChanges("{\"countryCode\":\"US\",\"countryName\":\"United States\"}");

        when(changeRequestRepository.findById(testId)).thenReturn(Optional.of(testChangeRequest));
        when(objectMapper.readValue(anyString(), eq(CountryDto.class))).thenReturn(testCountryDto);
        when(countryService.createCountryFromChangeRequest(any(CountryDto.class), anyString())).thenReturn(testCountryDto);
        when(changeRequestRepository.save(any(ChangeRequest.class))).thenReturn(testChangeRequest);

        // When
        ChangeRequest result = countryChangeRequestService.applyChangeRequest(testId);

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        ChangeRequest applied = captor.getValue();

        assertThat(applied.getStatus()).isEqualTo("APPLIED");
        assertThat(applied.getImplementedAt()).isNotNull();
        assertThat(applied.getImplementedBy()).isEqualTo("SYSTEM");

        // Verify that CountryService method was called for integration
        verify(objectMapper).readValue(testChangeRequest.getProposedChanges(), CountryDto.class);
        verify(countryService).validateCountryData(testCountryDto);
        verify(countryService).createCountryFromChangeRequest(testCountryDto, "SYSTEM");
    }

    @Test
    void applyChangeRequest_WithNonApprovedRequest_ShouldThrowException() {
        // Given
        testChangeRequest.setStatus("PENDING");
        when(changeRequestRepository.findById(testId)).thenReturn(Optional.of(testChangeRequest));

        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.applyChangeRequest(testId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Change request is not in APPROVED status");
    }

    @Test
    void applyChangeRequest_WithAlreadyAppliedRequest_ShouldThrowException() {
        // Given
        testChangeRequest.setStatus("APPLIED");
        when(changeRequestRepository.findById(testId)).thenReturn(Optional.of(testChangeRequest));

        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.applyChangeRequest(testId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Change request is not in APPROVED status");
    }

    @Test
    void getChangeRequestHistory_ShouldReturnHistoryForEntity() {
        // Given
        List<ChangeRequest> changeRequests = Arrays.asList(testChangeRequest);
        Page<ChangeRequest> page = new PageImpl<>(changeRequests, PageRequest.of(0, 10), 1);

        // Note: We'll need to add this method to the repository
        when(changeRequestRepository.findByFilters(
                eq(null), eq(null), eq("COUNTRY"), eq(null), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PagedResponse<ChangeRequest> result = countryChangeRequestService.getChangeRequestHistory(
                testId, PageRequest.of(0, 10));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(testId);
    }

    @Test
    void generateCrNumber_ShouldGenerateUniqueNumber() throws JsonProcessingException {
        // This tests a private method through a public method that uses it
        // Given
        String expectedJson = "{\"countryCode\":\"US\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);
        when(changeRequestRepository.save(any(ChangeRequest.class))).thenReturn(testChangeRequest);

        // When
        ChangeRequest result = countryChangeRequestService.createChangeRequest(
                testCountryDto, "CREATE", testUserId, testReason);

        // Then
        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        ChangeRequest saved = captor.getValue();

        assertThat(saved.getCrNumber()).matches("CR-\\d{4}-\\d{6}");
    }

    @Test
    void validateChangeRequestData_WithNullCountry_ShouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.createChangeRequest(
                null, "CREATE", testUserId, testReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country data is required");
    }

    @Test
    void validateChangeRequestData_WithBlankUserId_ShouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.createChangeRequest(
                testCountryDto, "CREATE", "", testReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID is required");
    }

    @Test
    void validateChangeRequestData_WithNullOperation_ShouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.createChangeRequest(
                testCountryDto, null, testUserId, testReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Operation type is required");
    }

    @Test
    void createChangeRequest_WithJsonSerializationError_ShouldThrowException() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization error") {});

        // When/Then
        assertThatThrownBy(() -> countryChangeRequestService.createChangeRequest(
                testCountryDto, "CREATE", testUserId, testReason))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Failed to serialize country data");
    }
}