package gov.dhs.cbp.reference.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.dto.CountryDto;
import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.repository.ChangeRequestRepository;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the complete change request workflow.
 * Tests the end-to-end flow from creation to approval to implementation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChangeRequestWorkflowIntegrationTest {

    @Autowired
    private CountryChangeRequestService countryChangeRequestService;

    @Autowired
    private ChangeRequestApplicationService applicationService;

    @Autowired
    private CountryService countryService;

    @Autowired
    private ChangeRequestRepository changeRequestRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private CodeSystemRepository codeSystemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private CodeSystem testCodeSystem;
    private String testUserId;
    private String approverUserId;
    private CountryDto testCountryDto;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-workflow";
        approverUserId = "approver-user-workflow";

        // Create test code system
        testCodeSystem = new CodeSystem();
        testCodeSystem.setId(UUID.randomUUID());
        testCodeSystem.setCode("TEST-WORKFLOW");
        testCodeSystem.setName("Test Workflow Code System");
        testCodeSystem.setDescription("Code system for workflow integration tests");
        testCodeSystem.setOwner("TEST");
        testCodeSystem.setCreatedAt(LocalDateTime.now());
        testCodeSystem.setUpdatedAt(LocalDateTime.now());
        codeSystemRepository.save(testCodeSystem);

        // Create test country DTO
        testCountryDto = new CountryDto();
        testCountryDto.setCountryCode("WT");
        testCountryDto.setCountryName("Workflow Test Country");
        testCountryDto.setIso2Code("WT");
        testCountryDto.setIso3Code("WTC");
        testCountryDto.setNumericCode("999");
        testCountryDto.setCodeSystem("TEST-WORKFLOW");
        testCountryDto.setIsActive(true);
        testCountryDto.setValidFrom(LocalDate.now());
        testCountryDto.setRecordedBy(testUserId);
    }

    @Test
    @Order(1)
    @DisplayName("Complete CREATE workflow: Request → Validation → Approval → Implementation")
    void testCompleteCreateWorkflow() throws Exception {
        // Step 1: Create change request
        ChangeRequest createRequest = countryChangeRequestService.createChangeRequest(
                testCountryDto, "CREATE", testUserId, "Creating new test country for workflow validation");

        assertThat(createRequest).isNotNull();
        assertThat(createRequest.getStatus()).isEqualTo("PENDING");
        assertThat(createRequest.getOperationType()).isEqualTo("CREATE");
        assertThat(createRequest.getDataType()).isEqualTo("COUNTRY");
        assertThat(createRequest.getCrNumber()).matches("CR-\\d{4}-\\d{6}");

        // Verify change request was saved
        Optional<ChangeRequest> savedRequest = changeRequestRepository.findById(createRequest.getId());
        assertThat(savedRequest).isPresent();

        // Step 2: Validate change request
        boolean isValid = applicationService.validateChangeRequest(createRequest.getId());
        assertThat(isValid).isTrue();

        // Step 3: Approve change request
        ChangeRequest approvedRequest = applicationService.processApproval(
                createRequest.getId(), approverUserId, "Approved for workflow test");

        assertThat(approvedRequest.getStatus()).isEqualTo("APPROVED");
        assertThat(approvedRequest.getApprovedBy()).isEqualTo(approverUserId);
        assertThat(approvedRequest.getApprovedAt()).isNotNull();

        // Step 4: Apply/implement change request
        ChangeRequest appliedRequest = applicationService.executeChangeRequest(createRequest.getId());

        assertThat(appliedRequest.getStatus()).isEqualTo("APPLIED");
        assertThat(appliedRequest.getImplementedAt()).isNotNull();
        assertThat(appliedRequest.getImplementedBy()).isEqualTo("SYSTEM");

        // Step 5: Verify country was created in the database
        Optional<Country> createdCountry = countryRepository.findByCountryCodeAndCodeSystemCode(
                testCountryDto.getCountryCode(), testCodeSystem.getCode());
        assertThat(createdCountry).isPresent();
        assertThat(createdCountry.get().getCountryName()).isEqualTo(testCountryDto.getCountryName());
        assertThat(createdCountry.get().getIsActive()).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Complete UPDATE workflow: Create → Update Request → Approval → Implementation")
    void testCompleteUpdateWorkflow() throws Exception {
        // Prerequisites: Create a country first
        Country existingCountry = createTestCountry();
        countryRepository.save(existingCountry);

        // Update the country data
        CountryDto updatedCountryDto = new CountryDto();
        updatedCountryDto.setId(existingCountry.getId());
        updatedCountryDto.setCountryCode(existingCountry.getCountryCode());
        updatedCountryDto.setCountryName("Updated Workflow Test Country");
        updatedCountryDto.setIso2Code(existingCountry.getIso2Code());
        updatedCountryDto.setIso3Code(existingCountry.getIso3Code());
        updatedCountryDto.setNumericCode(existingCountry.getNumericCode());
        updatedCountryDto.setCodeSystem(testCodeSystem.getCode());
        updatedCountryDto.setIsActive(true);
        updatedCountryDto.setValidFrom(LocalDate.now());
        updatedCountryDto.setRecordedBy(testUserId);

        // Step 1: Create update change request
        ChangeRequest updateRequest = countryChangeRequestService.createChangeRequest(
                updatedCountryDto, "UPDATE", testUserId, "Updating country name for clarity");

        assertThat(updateRequest.getOperationType()).isEqualTo("UPDATE");
        assertThat(updateRequest.getCurrentValues()).isNotNull();
        assertThat(updateRequest.getProposedChanges()).isNotNull();

        // Step 2: Validate and approve
        boolean isValid = applicationService.validateChangeRequest(updateRequest.getId());
        assertThat(isValid).isTrue();

        ChangeRequest approvedRequest = applicationService.processApproval(
                updateRequest.getId(), approverUserId, "Approved country name update");

        // Step 3: Apply changes
        ChangeRequest appliedRequest = applicationService.executeChangeRequest(updateRequest.getId());
        assertThat(appliedRequest.getStatus()).isEqualTo("APPLIED");

        // Step 4: Verify update was applied (new version created)
        Optional<Country> updatedCountry = countryRepository.findByCountryCodeAndCodeSystemCode(
                existingCountry.getCountryCode(), testCodeSystem.getCode());
        assertThat(updatedCountry).isPresent();
        assertThat(updatedCountry.get().getCountryName()).isEqualTo("Updated Workflow Test Country");
        assertThat(updatedCountry.get().getVersion()).isGreaterThan(existingCountry.getVersion());
    }

    @Test
    @Order(3)
    @DisplayName("Complete DELETE workflow: Create → Delete Request → Approval → Soft Delete")
    void testCompleteDeleteWorkflow() throws Exception {
        // Prerequisites: Create a country first
        Country existingCountry = createTestCountry();
        existingCountry.setCountryCode("DT"); // Delete Test
        existingCountry.setCountryName("Delete Test Country");
        countryRepository.save(existingCountry);

        CountryDto deleteCountryDto = new CountryDto();
        deleteCountryDto.setId(existingCountry.getId());
        deleteCountryDto.setCountryCode(existingCountry.getCountryCode());

        // Step 1: Create delete change request
        ChangeRequest deleteRequest = countryChangeRequestService.createChangeRequest(
                deleteCountryDto, "DELETE", testUserId, "Country is no longer needed");

        assertThat(deleteRequest.getOperationType()).isEqualTo("DELETE");
        assertThat(deleteRequest.getCurrentValues()).isNotNull();

        // Step 2: Validate and approve
        boolean isValid = applicationService.validateChangeRequest(deleteRequest.getId());
        assertThat(isValid).isTrue();

        ChangeRequest approvedRequest = applicationService.processApproval(
                deleteRequest.getId(), approverUserId, "Approved for deletion");

        // Step 3: Apply deletion (soft delete)
        ChangeRequest appliedRequest = applicationService.executeChangeRequest(deleteRequest.getId());
        assertThat(appliedRequest.getStatus()).isEqualTo("APPLIED");

        // Step 4: Verify soft delete was applied
        Optional<Country> deletedCountry = countryRepository.findById(existingCountry.getId());
        assertThat(deletedCountry).isPresent();
        assertThat(deletedCountry.get().getIsActive()).isFalse();
        assertThat(deletedCountry.get().getValidTo()).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("Rejection workflow: Request → Validation → Rejection")
    void testRejectionWorkflow() throws Exception {
        // Step 1: Create change request
        ChangeRequest rejectionRequest = countryChangeRequestService.createChangeRequest(
                testCountryDto, "CREATE", testUserId, "Test rejection workflow");

        // Step 2: Validate (should pass)
        boolean isValid = applicationService.validateChangeRequest(rejectionRequest.getId());
        assertThat(isValid).isTrue();

        // Step 3: Reject instead of approve
        ChangeRequest rejectedRequest = applicationService.processRejection(
                rejectionRequest.getId(), approverUserId, "Business requirements not met");

        assertThat(rejectedRequest.getStatus()).isEqualTo("REJECTED");
        assertThat(rejectedRequest.getRejectedBy()).isEqualTo(approverUserId);
        assertThat(rejectedRequest.getRejectedAt()).isNotNull();
        assertThat(rejectedRequest.getRejectionReason()).isEqualTo("Business requirements not met");

        // Step 4: Verify no country was created
        Optional<Country> shouldNotExist = countryRepository.findByCountryCodeAndCodeSystemCode(
                testCountryDto.getCountryCode(), testCodeSystem.getCode());
        assertThat(shouldNotExist).isEmpty();
    }

    @Test
    @Order(5)
    @DisplayName("Cancellation workflow: Request → Cancel")
    void testCancellationWorkflow() throws Exception {
        // Step 1: Create change request
        ChangeRequest cancellationRequest = countryChangeRequestService.createChangeRequest(
                testCountryDto, "CREATE", testUserId, "Test cancellation workflow");

        // Step 2: Cancel the request
        ChangeRequest cancelledRequest = applicationService.cancelChangeRequest(
                cancellationRequest.getId(), testUserId, "Requirements changed");

        assertThat(cancelledRequest.getStatus()).isEqualTo("CANCELLED");

        // Step 3: Verify no country was created
        Optional<Country> shouldNotExist = countryRepository.findByCountryCodeAndCodeSystemCode(
                testCountryDto.getCountryCode(), testCodeSystem.getCode());
        assertThat(shouldNotExist).isEmpty();
    }

    @Test
    @Order(6)
    @DisplayName("Scheduled implementation workflow")
    void testScheduledImplementationWorkflow() throws Exception {
        // Step 1: Create and approve change request
        ChangeRequest scheduledRequest = countryChangeRequestService.createChangeRequest(
                testCountryDto, "CREATE", testUserId, "Test scheduled implementation");

        applicationService.processApproval(scheduledRequest.getId(), approverUserId, "Approved for scheduled implementation");

        // Step 2: Schedule implementation for future date
        LocalDate futureDate = LocalDate.now().plusDays(7);
        ChangeRequest scheduledChangeRequest = applicationService.scheduleChangeRequest(
                scheduledRequest.getId(), futureDate);

        assertThat(scheduledChangeRequest.getEffectiveDate()).isEqualTo(futureDate);

        // Note: In a real system, a scheduler would execute this on the scheduled date
        // For testing, we manually trigger implementation
        ChangeRequest implementedRequest = applicationService.executeChangeRequest(scheduledRequest.getId());
        assertThat(implementedRequest.getStatus()).isEqualTo("APPLIED");
    }

    @Test
    @Order(7)
    @DisplayName("Invalid workflow transitions should fail")
    void testInvalidWorkflowTransitions() {
        // Create and approve a change request
        ChangeRequest approvedRequest = countryChangeRequestService.createChangeRequest(
                testCountryDto, "CREATE", testUserId, "Test invalid transitions");

        countryChangeRequestService.approveChangeRequest(approvedRequest.getId(), approverUserId, "Approved");

        // Try to approve again (should fail)
        assertThatThrownBy(() ->
            countryChangeRequestService.approveChangeRequest(approvedRequest.getId(), approverUserId, "Double approval"))
            .hasMessageContaining("not in PENDING status");

        // Try to reject an approved request (should fail)
        assertThatThrownBy(() ->
            countryChangeRequestService.rejectChangeRequest(approvedRequest.getId(), approverUserId, "Cannot reject approved"))
            .hasMessageContaining("not in PENDING status");

        // Execute the change request
        countryChangeRequestService.applyChangeRequest(approvedRequest.getId());

        // Try to execute again (should fail)
        assertThatThrownBy(() ->
            countryChangeRequestService.applyChangeRequest(approvedRequest.getId()))
            .hasMessageContaining("not in APPROVED status");
    }

    @Test
    @Order(8)
    @DisplayName("Concurrent workflow operations should handle properly")
    void testConcurrentWorkflowOperations() throws Exception {
        // Create change request
        ChangeRequest concurrentRequest = countryChangeRequestService.createChangeRequest(
                testCountryDto, "CREATE", testUserId, "Test concurrent operations");

        UUID requestId = concurrentRequest.getId();

        // Simulate concurrent approval attempts (in reality this would be from different threads)
        // First approval should succeed
        ChangeRequest firstApproval = applicationService.processApproval(
                requestId, "approver1", "First approval");
        assertThat(firstApproval.getStatus()).isEqualTo("APPROVED");

        // Second approval should fail since status is no longer PENDING
        assertThatThrownBy(() ->
            applicationService.processApproval(requestId, "approver2", "Second approval"))
            .hasMessageContaining("not in PENDING status");
    }

    @Test
    @Order(9)
    @DisplayName("Workflow with data validation errors")
    void testWorkflowWithValidationErrors() throws Exception {
        // Create country with invalid data
        CountryDto invalidCountryDto = new CountryDto();
        invalidCountryDto.setCountryCode(""); // Invalid: empty country code
        invalidCountryDto.setCountryName("Invalid Country");
        invalidCountryDto.setCodeSystem("TEST-WORKFLOW");

        // Creating the change request should still work (validation happens later)
        ChangeRequest invalidRequest = countryChangeRequestService.createChangeRequest(
                invalidCountryDto, "CREATE", testUserId, "Test with invalid data");

        // Validation should fail
        assertThatThrownBy(() ->
            applicationService.validateChangeRequest(invalidRequest.getId()))
            .hasMessageContaining("validation");
    }

    @Test
    @Order(10)
    @DisplayName("Workflow with business rule violations")
    void testWorkflowWithBusinessRuleViolations() throws Exception {
        // Try to update a non-existent country
        CountryDto nonExistentDto = new CountryDto();
        nonExistentDto.setId(UUID.randomUUID()); // Random UUID that doesn't exist
        nonExistentDto.setCountryCode("NE");
        nonExistentDto.setCountryName("Non-Existent Country");
        nonExistentDto.setCodeSystem("TEST-WORKFLOW");

        // Creating update request for non-existent country should fail
        assertThatThrownBy(() ->
            countryChangeRequestService.createChangeRequest(nonExistentDto, "UPDATE", testUserId, "Update non-existent"))
            .isInstanceOf(Exception.class);
    }

    // Helper methods
    private Country createTestCountry() {
        Country country = new Country();
        country.setId(UUID.randomUUID());
        country.setVersion(1L);
        country.setCodeSystem(testCodeSystem);
        country.setCountryCode("TC");
        country.setCountryName("Test Country");
        country.setIso2Code("TC");
        country.setIso3Code("TCC");
        country.setNumericCode("998");
        country.setValidFrom(LocalDate.now());
        country.setRecordedAt(LocalDateTime.now());
        country.setRecordedBy(testUserId);
        country.setIsActive(true);
        return country;
    }
}