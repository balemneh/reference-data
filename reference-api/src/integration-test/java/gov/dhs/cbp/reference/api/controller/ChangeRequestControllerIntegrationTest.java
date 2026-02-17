package gov.dhs.cbp.reference.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.ReferenceApiApplication;
import gov.dhs.cbp.reference.api.dto.CountryDto;
import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.ChangeRequestRepository;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Change Request API endpoints.
 * Tests the complete REST API workflow for change request management.
 */
@SpringBootTest(classes = ReferenceApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChangeRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChangeRequestRepository changeRequestRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private CodeSystemRepository codeSystemRepository;

    private CodeSystem testCodeSystem;
    private Country testCountry;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "test-api-user";

        // Clean up existing data
        changeRequestRepository.deleteAll();
        countryRepository.deleteAll();
        codeSystemRepository.deleteAll();

        // Create test code system
        testCodeSystem = new CodeSystem();
        testCodeSystem.setId(UUID.randomUUID());
        testCodeSystem.setCode("TEST-API");
        testCodeSystem.setName("Test API Code System");
        testCodeSystem.setDescription("Code system for API integration tests");
        testCodeSystem.setOwner("TEST");
        testCodeSystem.setCreatedAt(LocalDateTime.now());
        testCodeSystem.setUpdatedAt(LocalDateTime.now());
        codeSystemRepository.save(testCodeSystem);

        // Create test country
        testCountry = new Country();
        testCountry.setId(UUID.randomUUID());
        testCountry.setVersion(1L);
        testCodeSystem = codeSystemRepository.save(testCodeSystem);
        testCountry.setCodeSystem(testCodeSystem);
        testCountry.setCountryCode("API");
        testCountry.setCountryName("API Test Country");
        testCountry.setIso2Code("AP");
        testCountry.setIso3Code("API");
        testCountry.setNumericCode("997");
        testCountry.setValidFrom(LocalDate.now());
        testCountry.setRecordedAt(LocalDateTime.now());
        testCountry.setRecordedBy(testUserId);
        testCountry.setIsActive(true);
        countryRepository.save(testCountry);
    }

    @Test
    @Order(1)
    @DisplayName("POST /v1/change-requests/countries should create country change request")
    void testCreateCountryChangeRequest() throws Exception {
        // Given
        String requestJson = """
            {
                "countryData": {
                    "countryCode": "NW",
                    "countryName": "New World",
                    "iso2Code": "NW",
                    "iso3Code": "NWD",
                    "numericCode": "996",
                    "codeSystem": "TEST-API",
                    "isActive": true
                },
                "operationType": "CREATE",
                "businessJustification": "Adding new country for testing API workflow",
                "requesterId": "test-api-user",
                "priority": "MEDIUM"
            }
            """;

        // When & Then
        MvcResult result = mockMvc.perform(post("/v1/change-requests/countries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.operationType").value("CREATE"))
                .andExpect(jsonPath("$.dataType").value("COUNTRY"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requesterId").value("test-api-user"))
                .andExpect(jsonPath("$.businessJustification").value("Adding new country for testing API workflow"))
                .andExpect(jsonPath("$.crNumber").exists())
                .andReturn();

        // Verify change request was saved to database
        String locationHeader = result.getResponse().getHeader("Location");
        assertThat(locationHeader).isNotNull();

        String requestId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
        UUID uuid = UUID.fromString(requestId);

        ChangeRequest savedRequest = changeRequestRepository.findById(uuid).orElse(null);
        assertThat(savedRequest).isNotNull();
        assertThat(savedRequest.getStatus()).isEqualTo("PENDING");
        assertThat(savedRequest.getOperationType()).isEqualTo("CREATE");
    }

    @Test
    @Order(2)
    @DisplayName("POST /v1/change-requests/countries should validate required fields")
    void testCreateChangeRequestValidation() throws Exception {
        // Given - Missing required fields
        String invalidRequestJson = """
            {
                "countryData": {
                    "countryCode": "",
                    "countryName": ""
                },
                "operationType": "CREATE"
            }
            """;

        // When & Then
        mockMvc.perform(post("/v1/change-requests/countries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("GET /v1/change-requests should return paginated change requests")
    void testGetChangeRequestsPaginated() throws Exception {
        // Given - Create some test change requests
        createTestChangeRequests();

        // When & Then
        mockMvc.perform(get("/v1/change-requests")
                .param("page", "0")
                .param("size", "2")
                .param("status", "PENDING"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    @Order(4)
    @DisplayName("GET /v1/change-requests/{id} should return specific change request")
    void testGetChangeRequestById() throws Exception {
        // Given
        ChangeRequest testRequest = createSampleChangeRequest("GET", "Test Get Request");
        ChangeRequest savedRequest = changeRequestRepository.save(testRequest);

        // When & Then
        mockMvc.perform(get("/v1/change-requests/{id}", savedRequest.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(savedRequest.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.operationType").value("CREATE"))
                .andExpect(jsonPath("$.dataType").value("COUNTRY"))
                .andExpect(jsonPath("$.crNumber").exists());
    }

    @Test
    @Order(5)
    @DisplayName("GET /v1/change-requests/{id} should return 404 for non-existent request")
    void testGetChangeRequestByIdNotFound() throws Exception {
        // When & Then
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/v1/change-requests/{id}", nonExistentId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Change request not found"))
                .andExpect(jsonPath("$.detail").value("Change request with id " + nonExistentId + " not found"));
    }

    @Test
    @Order(6)
    @DisplayName("PUT /v1/change-requests/{id}/approve should approve change request")
    void testApproveChangeRequest() throws Exception {
        // Given
        ChangeRequest testRequest = createSampleChangeRequest("APPROVE", "Test Approval");
        ChangeRequest savedRequest = changeRequestRepository.save(testRequest);

        String approvalJson = """
            {
                "approverUserId": "approver-123",
                "comments": "Approved for implementation",
                "priority": "HIGH"
            }
            """;

        // When & Then
        mockMvc.perform(put("/v1/change-requests/{id}/approve", savedRequest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(approvalJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value("approver-123"))
                .andExpect(jsonPath("$.approvedAt").exists())
                .andExpect(jsonPath("$.priority").value("HIGH"));

        // Verify in database
        ChangeRequest approvedRequest = changeRequestRepository.findById(savedRequest.getId()).orElse(null);
        assertThat(approvedRequest).isNotNull();
        assertThat(approvedRequest.getStatus()).isEqualTo("APPROVED");
        assertThat(approvedRequest.getApprovedBy()).isEqualTo("approver-123");
    }

    @Test
    @Order(7)
    @DisplayName("PUT /v1/change-requests/{id}/reject should reject change request")
    void testRejectChangeRequest() throws Exception {
        // Given
        ChangeRequest testRequest = createSampleChangeRequest("REJECT", "Test Rejection");
        ChangeRequest savedRequest = changeRequestRepository.save(testRequest);

        String rejectionJson = """
            {
                "rejectorUserId": "rejector-123",
                "reason": "Business requirements not met",
                "comments": "Please revise and resubmit"
            }
            """;

        // When & Then
        mockMvc.perform(put("/v1/change-requests/{id}/reject", savedRequest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectionJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectedBy").value("rejector-123"))
                .andExpect(jsonPath("$.rejectedAt").exists())
                .andExpect(jsonPath("$.rejectionReason").value("Business requirements not met"));
    }

    @Test
    @Order(8)
    @DisplayName("POST /v1/change-requests/{id}/execute should implement approved change request")
    void testExecuteChangeRequest() throws Exception {
        // Given - Create and approve a change request
        ChangeRequest testRequest = createSampleChangeRequest("EXECUTE", "Test Execution");
        testRequest.setStatus("APPROVED");
        testRequest.setApprovedBy("approver-123");
        testRequest.setApprovedAt(LocalDateTime.now());
        ChangeRequest savedRequest = changeRequestRepository.save(testRequest);

        // When & Then
        mockMvc.perform(post("/v1/change-requests/{id}/execute", savedRequest.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.implementedAt").exists())
                .andExpect(jsonPath("$.implementedBy").value("SYSTEM"));

        // Verify implementation created the country
        assertThat(countryRepository.findByCountryCodeAndSystemCode("EX", "TEST-API")).isPresent();
    }

    @Test
    @Order(9)
    @DisplayName("POST /v1/change-requests/bulk/approve should approve multiple requests")
    void testBulkApproveChangeRequests() throws Exception {
        // Given
        ChangeRequest request1 = createSampleChangeRequest("BULK1", "Test Bulk 1");
        ChangeRequest request2 = createSampleChangeRequest("BULK2", "Test Bulk 2");
        ChangeRequest savedRequest1 = changeRequestRepository.save(request1);
        ChangeRequest savedRequest2 = changeRequestRepository.save(request2);

        String bulkApprovalJson = String.format("""
            {
                "requestIds": ["%s", "%s"],
                "approverUserId": "bulk-approver",
                "comments": "Bulk approval for testing"
            }
            """, savedRequest1.getId(), savedRequest2.getId());

        // When & Then
        mockMvc.perform(post("/v1/change-requests/bulk/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkApprovalJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[1].status").value("APPROVED"));
    }

    @Test
    @Order(10)
    @DisplayName("GET /v1/change-requests/metrics should return workflow metrics")
    void testGetChangeRequestMetrics() throws Exception {
        // Given
        createTestChangeRequests();

        // When & Then
        mockMvc.perform(get("/v1/change-requests/metrics"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pendingCount").exists())
                .andExpect(jsonPath("$.approvedCount").exists())
                .andExpect(jsonPath("$.rejectedCount").exists())
                .andExpect(jsonPath("$.totalCount").exists())
                .andExpect(jsonPath("$.pendingCount").value(3)); // 3 from createTestChangeRequests
    }

    @Test
    @Order(11)
    @DisplayName("GET /v1/change-requests/history should return audit trail")
    void testGetChangeRequestHistory() throws Exception {
        // Given
        ChangeRequest request = createSampleChangeRequest("HISTORY", "Test History");
        ChangeRequest savedRequest = changeRequestRepository.save(request);

        // When & Then
        mockMvc.perform(get("/v1/change-requests/history")
                .param("entityId", testCountry.getId().toString())
                .param("dataType", "COUNTRY"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(12)
    @DisplayName("POST /v1/change-requests/{id}/schedule should schedule implementation")
    void testScheduleChangeRequest() throws Exception {
        // Given
        ChangeRequest testRequest = createSampleChangeRequest("SCHEDULE", "Test Scheduling");
        testRequest.setStatus("APPROVED");
        testRequest.setApprovedBy("approver-123");
        testRequest.setApprovedAt(LocalDateTime.now());
        ChangeRequest savedRequest = changeRequestRepository.save(testRequest);

        LocalDate futureDate = LocalDate.now().plusDays(7);
        String scheduleJson = String.format("""
            {
                "effectiveDate": "%s",
                "scheduledBy": "scheduler-123"
            }
            """, futureDate);

        // When & Then
        mockMvc.perform(post("/v1/change-requests/{id}/schedule", savedRequest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveDate").value(futureDate.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @Order(13)
    @DisplayName("Should handle invalid workflow state transitions")
    void testInvalidWorkflowTransitions() throws Exception {
        // Given - Already approved request
        ChangeRequest approvedRequest = createSampleChangeRequest("INVALID", "Test Invalid Transitions");
        approvedRequest.setStatus("APPROVED");
        ChangeRequest savedRequest = changeRequestRepository.save(approvedRequest);

        String approvalJson = """
            {
                "approverUserId": "double-approver",
                "comments": "Trying to approve again"
            }
            """;

        // When & Then - Try to approve already approved request
        mockMvc.perform(put("/v1/change-requests/{id}/approve", savedRequest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(approvalJson))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid workflow state"))
                .andExpect(jsonPath("$.detail").value(containsString("not in PENDING status")));
    }

    @Test
    @Order(14)
    @DisplayName("Should support filtering and searching change requests")
    void testFilterAndSearchChangeRequests() throws Exception {
        // Given
        createTestChangeRequests();

        // When & Then - Filter by status
        mockMvc.perform(get("/v1/change-requests")
                .param("status", "PENDING")
                .param("dataType", "COUNTRY"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].status").value(
                    org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("PENDING"))));

        // When & Then - Filter by requester
        mockMvc.perform(get("/v1/change-requests")
                .param("requesterId", testUserId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].requesterId").value(
                    org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(testUserId))));
    }

    @Test
    @Order(15)
    @DisplayName("Should support ETag caching for change requests")
    void testETagSupportForChangeRequests() throws Exception {
        // Given
        ChangeRequest testRequest = createSampleChangeRequest("ETAG", "Test ETag Support");
        ChangeRequest savedRequest = changeRequestRepository.save(testRequest);

        // When - First request
        MvcResult firstResult = mockMvc.perform(get("/v1/change-requests/{id}", savedRequest.getId()))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andReturn();

        String etag = firstResult.getResponse().getHeader("ETag");
        assertThat(etag).isNotNull();

        // Then - Subsequent request with ETag should return 304 Not Modified
        mockMvc.perform(get("/v1/change-requests/{id}", savedRequest.getId())
                .header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    // Helper methods
    private void createTestChangeRequests() {
        for (int i = 1; i <= 3; i++) {
            ChangeRequest request = createSampleChangeRequest("TEST" + i, "Test Request " + i);
            changeRequestRepository.save(request);
        }
    }

    private ChangeRequest createSampleChangeRequest(String countryCode, String description) {
        ChangeRequest request = new ChangeRequest();
        request.setId(UUID.randomUUID());
        request.setCrNumber(generateCrNumber());
        request.setTitle("Create new country: " + countryCode);
        request.setDescription(description);
        request.setOperationType("CREATE");
        request.setDataType("COUNTRY");
        request.setStatus("PENDING");
        request.setRequesterId(testUserId);
        request.setBusinessJustification("Test business justification for " + description);
        request.setPriority("MEDIUM");
        request.setCreatedAt(LocalDateTime.now());
        request.setSubmittedAt(LocalDateTime.now());

        // Create proposed changes JSON
        CountryDto proposedCountry = new CountryDto();
        proposedCountry.setCountryCode(countryCode);
        proposedCountry.setCountryName(description + " Country");
        proposedCountry.setIso2Code(countryCode.substring(0, Math.min(2, countryCode.length())));
        proposedCountry.setIso3Code(countryCode);
        proposedCountry.setNumericCode("995");
        proposedCountry.setCodeSystem("TEST-API");
        proposedCountry.setIsActive(true);

        try {
            String proposedChangesJson = objectMapper.writeValueAsString(proposedCountry);
            request.setProposedChanges(proposedChangesJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize test data", e);
        }

        return request;
    }

    private String generateCrNumber() {
        return String.format("CR-%d-%06d",
            LocalDateTime.now().getYear(),
            (int) (Math.random() * 1000000));
    }
}