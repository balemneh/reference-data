package gov.dhs.cbp.reference.api.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.dto.CountryDto;
import gov.dhs.cbp.reference.api.service.ChangeRequestApplicationService;
import gov.dhs.cbp.reference.api.service.CountryChangeRequestService;
import gov.dhs.cbp.reference.api.service.CountryService;
import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.ChangeRequestRepository;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance benchmark tests for the Change Request Workflow system.
 * These tests measure performance under various load conditions and establish baseline metrics.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChangeRequestWorkflowPerformanceTest {

    @Autowired
    private ChangeRequestApplicationService applicationService;

    @Autowired
    private CountryChangeRequestService countryChangeRequestService;

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
    private ExecutorService executorService;
    private static final String TEST_USER = "performance-test-user";
    private static final String APPROVER_USER = "performance-approver";

    // Performance thresholds (adjust based on requirements)
    private static final long SINGLE_CR_CREATION_THRESHOLD_MS = 1000; // 1 second
    private static final long SINGLE_CR_APPROVAL_THRESHOLD_MS = 500;  // 0.5 seconds
    private static final long SINGLE_CR_EXECUTION_THRESHOLD_MS = 2000; // 2 seconds
    private static final long BULK_OPERATION_THRESHOLD_MS = 5000;      // 5 seconds for 100 items
    private static final long CONCURRENT_OPERATION_THRESHOLD_MS = 10000; // 10 seconds for 50 concurrent ops

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(50);

        // Create test code system
        testCodeSystem = new CodeSystem();
        testCodeSystem.setId(UUID.randomUUID());
        testCodeSystem.setCode("PERF-TEST");
        testCodeSystem.setName("Performance Test Code System");
        testCodeSystem.setDescription("Code system for performance testing");
        testCodeSystem.setOwner("PERFORMANCE");
        testCodeSystem.setCreatedAt(LocalDateTime.now());
        testCodeSystem.setUpdatedAt(LocalDateTime.now());
        codeSystemRepository.save(testCodeSystem);
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Performance: Single Change Request Creation")
    void testSingleChangeRequestCreationPerformance() throws Exception {
        // Given
        CountryDto countryDto = createTestCountryDto("PERF001");
        StopWatch stopWatch = new StopWatch("Single Change Request Creation");

        // When
        stopWatch.start();
        ChangeRequest result = countryChangeRequestService.createChangeRequest(
                countryDto, "CREATE", TEST_USER, "Performance test - single creation");
        stopWatch.stop();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PENDING");

        long executionTime = stopWatch.getTotalTimeMillis();
        System.out.printf("Single Change Request Creation: %d ms%n", executionTime);

        assertThat(executionTime)
                .as("Single change request creation should complete within %d ms, actual: %d ms",
                        SINGLE_CR_CREATION_THRESHOLD_MS, executionTime)
                .isLessThan(SINGLE_CR_CREATION_THRESHOLD_MS);
    }

    @Test
    @Order(2)
    @DisplayName("Performance: Single Change Request Approval")
    void testSingleChangeRequestApprovalPerformance() throws Exception {
        // Given
        CountryDto countryDto = createTestCountryDto("PERF002");
        ChangeRequest changeRequest = countryChangeRequestService.createChangeRequest(
                countryDto, "CREATE", TEST_USER, "Performance test - single approval");

        StopWatch stopWatch = new StopWatch("Single Change Request Approval");

        // When
        stopWatch.start();
        ChangeRequest result = applicationService.processApproval(
                changeRequest.getId(), APPROVER_USER, "Performance test approval");
        stopWatch.stop();

        // Then
        assertThat(result.getStatus()).isEqualTo("APPROVED");

        long executionTime = stopWatch.getTotalTimeMillis();
        System.out.printf("Single Change Request Approval: %d ms%n", executionTime);

        assertThat(executionTime)
                .as("Single change request approval should complete within %d ms, actual: %d ms",
                        SINGLE_CR_APPROVAL_THRESHOLD_MS, executionTime)
                .isLessThan(SINGLE_CR_APPROVAL_THRESHOLD_MS);
    }

    @Test
    @Order(3)
    @DisplayName("Performance: Single Change Request Execution")
    void testSingleChangeRequestExecutionPerformance() throws Exception {
        // Given
        CountryDto countryDto = createTestCountryDto("PERF003");
        ChangeRequest changeRequest = countryChangeRequestService.createChangeRequest(
                countryDto, "CREATE", TEST_USER, "Performance test - single execution");

        applicationService.processApproval(changeRequest.getId(), APPROVER_USER, "Approved for performance test");

        StopWatch stopWatch = new StopWatch("Single Change Request Execution");

        // When
        stopWatch.start();
        ChangeRequest result = applicationService.executeChangeRequest(changeRequest.getId());
        stopWatch.stop();

        // Then
        assertThat(result.getStatus()).isEqualTo("APPLIED");

        long executionTime = stopWatch.getTotalTimeMillis();
        System.out.printf("Single Change Request Execution: %d ms%n", executionTime);

        assertThat(executionTime)
                .as("Single change request execution should complete within %d ms, actual: %d ms",
                        SINGLE_CR_EXECUTION_THRESHOLD_MS, executionTime)
                .isLessThan(SINGLE_CR_EXECUTION_THRESHOLD_MS);

        // Verify country was created
        Optional<Country> createdCountry = countryRepository.findByCountryCodeAndCodeSystemCode(
                "PERF003", testCodeSystem.getCode());
        assertThat(createdCountry).isPresent();
    }

    @Test
    @Order(4)
    @DisplayName("Performance: Bulk Change Request Operations")
    @Transactional
    void testBulkChangeRequestOperationsPerformance() throws Exception {
        // Given
        int bulkSize = 100;
        List<ChangeRequest> changeRequests = new ArrayList<>();

        StopWatch creationStopWatch = new StopWatch("Bulk Change Request Creation");

        // When - Create bulk change requests
        creationStopWatch.start();
        for (int i = 1; i <= bulkSize; i++) {
            CountryDto countryDto = createTestCountryDto("BULK" + String.format("%03d", i));
            ChangeRequest request = countryChangeRequestService.createChangeRequest(
                    countryDto, "CREATE", TEST_USER, "Bulk performance test " + i);
            changeRequests.add(request);
        }
        creationStopWatch.stop();

        long creationTime = creationStopWatch.getTotalTimeMillis();
        System.out.printf("Bulk Creation (%d items): %d ms (%.2f ms per item)%n",
                bulkSize, creationTime, (double) creationTime / bulkSize);

        // When - Bulk approve
        StopWatch approvalStopWatch = new StopWatch("Bulk Change Request Approval");
        List<UUID> requestIds = changeRequests.stream().map(ChangeRequest::getId).toList();

        approvalStopWatch.start();
        List<ChangeRequest> approvedRequests = applicationService.bulkApprove(requestIds, APPROVER_USER);
        approvalStopWatch.stop();

        long approvalTime = approvalStopWatch.getTotalTimeMillis();
        System.out.printf("Bulk Approval (%d items): %d ms (%.2f ms per item)%n",
                bulkSize, approvalTime, (double) approvalTime / bulkSize);

        // Then
        assertThat(approvedRequests).hasSize(bulkSize);
        assertThat(approvedRequests).allMatch(cr -> "APPROVED".equals(cr.getStatus()));

        long totalBulkTime = creationTime + approvalTime;
        assertThat(totalBulkTime)
                .as("Bulk operations (%d items) should complete within %d ms, actual: %d ms",
                        bulkSize, BULK_OPERATION_THRESHOLD_MS, totalBulkTime)
                .isLessThan(BULK_OPERATION_THRESHOLD_MS);
    }

    @Test
    @Order(5)
    @DisplayName("Performance: Concurrent Change Request Operations")
    void testConcurrentChangeRequestOperationsPerformance() throws Exception {
        // Given
        int concurrentThreads = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(concurrentThreads);
        List<Future<ChangeRequest>> futures = new ArrayList<>();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        StopWatch stopWatch = new StopWatch("Concurrent Change Request Operations");

        // When - Submit concurrent change request creation tasks
        for (int i = 1; i <= concurrentThreads; i++) {
            final int threadId = i;
            Future<ChangeRequest> future = executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    CountryDto countryDto = createTestCountryDto("CONC" + String.format("%03d", threadId));
                    return countryChangeRequestService.createChangeRequest(
                            countryDto, "CREATE", TEST_USER + threadId, "Concurrent test " + threadId);
                } catch (Exception e) {
                    exceptions.add(e);
                    throw new RuntimeException(e);
                } finally {
                    finishLatch.countDown();
                }
            });
            futures.add(future);
        }

        // Start all threads simultaneously
        stopWatch.start();
        startLatch.countDown();

        // Wait for all to complete
        boolean completed = finishLatch.await(CONCURRENT_OPERATION_THRESHOLD_MS, TimeUnit.MILLISECONDS);
        stopWatch.stop();

        // Then
        assertThat(completed)
                .as("All concurrent operations should complete within %d ms", CONCURRENT_OPERATION_THRESHOLD_MS)
                .isTrue();

        assertThat(exceptions)
                .as("No exceptions should occur during concurrent operations")
                .isEmpty();

        // Verify all change requests were created successfully
        List<ChangeRequest> results = new ArrayList<>();
        for (Future<ChangeRequest> future : futures) {
            if (future.isDone() && !future.isCancelled()) {
                try {
                    results.add(future.get(1, TimeUnit.SECONDS));
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        }

        assertThat(results).hasSize(concurrentThreads);
        assertThat(results).allMatch(cr -> "PENDING".equals(cr.getStatus()));

        long executionTime = stopWatch.getTotalTimeMillis();
        System.out.printf("Concurrent Operations (%d threads): %d ms (%.2f ms per operation)%n",
                concurrentThreads, executionTime, (double) executionTime / concurrentThreads);

        assertThat(executionTime)
                .as("Concurrent operations (%d threads) should complete within %d ms, actual: %d ms",
                        concurrentThreads, CONCURRENT_OPERATION_THRESHOLD_MS, executionTime)
                .isLessThan(CONCURRENT_OPERATION_THRESHOLD_MS);
    }

    @Test
    @Order(6)
    @DisplayName("Performance: Database Query Performance")
    void testDatabaseQueryPerformance() {
        // Given - Create test data
        List<ChangeRequest> testRequests = createTestChangeRequests(200);
        changeRequestRepository.saveAll(testRequests);

        StopWatch stopWatch = new StopWatch("Database Query Performance");

        // Test pagination query performance
        stopWatch.start("Paginated Query");
        var pagedResult = applicationService.getChangeRequestsForApproval(APPROVER_USER,
                org.springframework.data.domain.PageRequest.of(0, 20));
        stopWatch.stop();

        // Test status filter query performance
        stopWatch.start("Status Filter Query");
        var pendingRequests = countryChangeRequestService.getPendingChangeRequests(
                org.springframework.data.domain.PageRequest.of(0, 50));
        stopWatch.stop();

        // Test search query performance
        stopWatch.start("Search Query");
        var searchResults = changeRequestRepository.findByFilters(
                null, "PENDING", "COUNTRY", null, LocalDateTime.now().minusHours(24),
                org.springframework.data.domain.PageRequest.of(0, 20));
        stopWatch.stop();

        // Then
        assertThat(pagedResult.getContent()).isNotEmpty();
        assertThat(pendingRequests.getContent()).isNotEmpty();
        assertThat(searchResults.getContent()).isNotEmpty();

        System.out.println("Database Query Performance:");
        System.out.println(stopWatch.prettyPrint());

        // Verify each query completes within reasonable time
        stopWatch.getTaskInfo().forEach(task -> {
            assertThat(task.getTimeMillis())
                    .as("%s should complete within 2 seconds", task.getTaskName())
                    .isLessThan(2000);
        });
    }

    @Test
    @Order(7)
    @DisplayName("Performance: Memory Usage Under Load")
    void testMemoryUsageUnderLoad() {
        // Given
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        int loadSize = 1000;
        List<ChangeRequest> requests = new ArrayList<>();

        // When - Create large number of change requests
        StopWatch stopWatch = new StopWatch("Memory Load Test");
        stopWatch.start();

        for (int i = 1; i <= loadSize; i++) {
            CountryDto countryDto = createTestCountryDto("MEM" + String.format("%04d", i));
            ChangeRequest request = countryChangeRequestService.createChangeRequest(
                    countryDto, "CREATE", TEST_USER, "Memory test " + i);
            requests.add(request);

            // Force garbage collection every 100 items
            if (i % 100 == 0) {
                System.gc();
                Thread.yield();
            }
        }

        stopWatch.stop();

        // Then
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        long memoryPerRequest = memoryIncrease / loadSize;

        System.out.printf("Memory Usage Test (%d items):%n", loadSize);
        System.out.printf("  Initial Memory: %d KB%n", initialMemory / 1024);
        System.out.printf("  Final Memory: %d KB%n", finalMemory / 1024);
        System.out.printf("  Memory Increase: %d KB%n", memoryIncrease / 1024);
        System.out.printf("  Memory per Request: %d bytes%n", memoryPerRequest);
        System.out.printf("  Execution Time: %d ms%n", stopWatch.getTotalTimeMillis());

        assertThat(requests).hasSize(loadSize);

        // Memory should not increase by more than 100MB for 1000 requests
        assertThat(memoryIncrease)
                .as("Memory increase should be reasonable (< 100MB for %d requests)", loadSize)
                .isLessThan(100 * 1024 * 1024); // 100MB
    }

    @Test
    @Order(8)
    @DisplayName("Performance: End-to-End Workflow Performance")
    void testEndToEndWorkflowPerformance() throws Exception {
        // Given
        CountryDto countryDto = createTestCountryDto("E2E001");
        StopWatch workflowStopWatch = new StopWatch("End-to-End Workflow");

        // When - Complete workflow
        workflowStopWatch.start("1. Create Change Request");
        ChangeRequest createRequest = countryChangeRequestService.createChangeRequest(
                countryDto, "CREATE", TEST_USER, "End-to-end performance test");
        workflowStopWatch.stop();

        workflowStopWatch.start("2. Validate Change Request");
        boolean isValid = applicationService.validateChangeRequest(createRequest.getId());
        workflowStopWatch.stop();

        workflowStopWatch.start("3. Approve Change Request");
        ChangeRequest approvedRequest = applicationService.processApproval(
                createRequest.getId(), APPROVER_USER, "E2E approval");
        workflowStopWatch.stop();

        workflowStopWatch.start("4. Execute Change Request");
        ChangeRequest appliedRequest = applicationService.executeChangeRequest(createRequest.getId());
        workflowStopWatch.stop();

        workflowStopWatch.start("5. Verify Country Creation");
        Optional<Country> createdCountry = countryRepository.findByCountryCodeAndCodeSystemCode(
                "E2E001", testCodeSystem.getCode());
        workflowStopWatch.stop();

        // Then
        assertThat(isValid).isTrue();
        assertThat(appliedRequest.getStatus()).isEqualTo("APPLIED");
        assertThat(createdCountry).isPresent();

        System.out.println("End-to-End Workflow Performance:");
        System.out.println(workflowStopWatch.prettyPrint());

        long totalWorkflowTime = workflowStopWatch.getTotalTimeMillis();
        System.out.printf("Total Workflow Time: %d ms%n", totalWorkflowTime);

        // Complete workflow should finish within 5 seconds
        assertThat(totalWorkflowTime)
                .as("Complete workflow should finish within 5 seconds, actual: %d ms", totalWorkflowTime)
                .isLessThan(5000);
    }

    @Test
    @Order(9)
    @DisplayName("Performance: Stress Test - High Volume Operations")
    void testHighVolumeStressTest() throws Exception {
        // Given
        int stressTestSize = 500;
        int batchSize = 50;
        List<Long> batchTimes = new ArrayList<>();

        StopWatch stressStopWatch = new StopWatch("High Volume Stress Test");

        // When - Process in batches to avoid memory issues
        stressStopWatch.start();

        for (int batch = 0; batch < stressTestSize / batchSize; batch++) {
            StopWatch batchStopWatch = new StopWatch("Batch " + (batch + 1));
            batchStopWatch.start();

            List<CompletableFuture<ChangeRequest>> batchFutures = new ArrayList<>();

            for (int i = 0; i < batchSize; i++) {
                int index = batch * batchSize + i;
                CompletableFuture<ChangeRequest> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        CountryDto countryDto = createTestCountryDto("STRESS" + String.format("%04d", index));
                        return countryChangeRequestService.createChangeRequest(
                                countryDto, "CREATE", TEST_USER, "Stress test " + index);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create change request", e);
                    }
                }, executorService);
                batchFutures.add(future);
            }

            // Wait for batch to complete
            CompletableFuture<Void> batchCompletion = CompletableFuture.allOf(
                    batchFutures.toArray(new CompletableFuture[0]));
            batchCompletion.get(30, TimeUnit.SECONDS);

            batchStopWatch.stop();
            batchTimes.add(batchStopWatch.getTotalTimeMillis());

            System.out.printf("Batch %d completed in %d ms%n", batch + 1, batchStopWatch.getTotalTimeMillis());

            // Brief pause between batches to allow system recovery
            Thread.sleep(100);
        }

        stressStopWatch.stop();

        // Then
        long totalStressTime = stressStopWatch.getTotalTimeMillis();
        double avgBatchTime = batchTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double throughput = (double) stressTestSize / (totalStressTime / 1000.0); // operations per second

        System.out.printf("Stress Test Results:%n");
        System.out.printf("  Total Operations: %d%n", stressTestSize);
        System.out.printf("  Total Time: %d ms%n", totalStressTime);
        System.out.printf("  Average Batch Time: %.2f ms%n", avgBatchTime);
        System.out.printf("  Throughput: %.2f operations/second%n", throughput);

        // Verify reasonable throughput (at least 10 operations per second)
        assertThat(throughput)
                .as("System should maintain at least 10 operations per second under stress, actual: %.2f", throughput)
                .isGreaterThan(10.0);

        // Verify system completed the stress test
        assertThat(totalStressTime)
                .as("Stress test should complete within reasonable time")
                .isLessThan(60000); // 1 minute
    }

    // Helper methods
    private CountryDto createTestCountryDto(String code) {
        CountryDto dto = new CountryDto();
        dto.setCountryCode(code);
        dto.setCountryName("Performance Test Country " + code);
        dto.setIso2Code(code.substring(0, Math.min(2, code.length())));
        dto.setIso3Code(code.substring(0, Math.min(3, code.length())));
        dto.setNumericCode("999");
        dto.setCodeSystem(testCodeSystem.getCode());
        dto.setIsActive(true);
        dto.setValidFrom(LocalDate.now());
        dto.setRecordedBy(TEST_USER);
        return dto;
    }

    private List<ChangeRequest> createTestChangeRequests(int count) {
        return IntStream.range(1, count + 1)
                .mapToObj(i -> {
                    ChangeRequest request = new ChangeRequest();
                    request.setId(UUID.randomUUID());
                    request.setCrNumber("CR-2025-" + String.format("%06d", i));
                    request.setTitle("Performance Test Request " + i);
                    request.setDescription("Test request for performance testing");
                    request.setOperationType("CREATE");
                    request.setDataType("COUNTRY");
                    request.setStatus("PENDING");
                    request.setRequesterId(TEST_USER);
                    request.setBusinessJustification("Performance test justification " + i);
                    request.setPriority("MEDIUM");
                    request.setCreatedAt(LocalDateTime.now().minusMinutes(i));
                    request.setSubmittedAt(LocalDateTime.now().minusMinutes(i));
                    request.setProposedChanges("{\"countryCode\":\"PERF" + i + "\",\"countryName\":\"Performance Test " + i + "\"}");
                    return request;
                })
                .toList();
    }
}