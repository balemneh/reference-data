package gov.dhs.cbp.reference.api.service;

import gov.dhs.cbp.reference.core.entity.BulkImportBatch;
import gov.dhs.cbp.reference.core.repository.BulkImportBatchRepository;
import gov.dhs.cbp.reference.core.repository.BulkImportStagingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AsyncBulkImportServiceIntegrationTest {

    @Autowired
    private AsyncBulkImportService asyncBulkImportService;

    @Autowired
    private BulkImportBatchRepository batchRepository;

    @Autowired
    private BulkImportStagingRepository stagingRepository;

    @MockBean
    private ChangeRequestService changeRequestService;

    private UUID testBatchId;
    private BulkImportBatch testBatch;

    @BeforeEach
    void setUp() {
        testBatchId = UUID.randomUUID();
        
        testBatch = new BulkImportBatch();
        testBatch.setId(testBatchId);
        testBatch.setBatchName("TEST_BATCH");
        testBatch.setChangeRequestId(UUID.randomUUID());
        testBatch.setDataType(BulkImportBatch.DataType.COUNTRIES);
        testBatch.setStatus(BulkImportBatch.BatchStatus.PENDING);
        testBatch.setSourceSystem("TEST_SYSTEM");
        testBatch.setCreatedBy("test-user");
        testBatch.setCreatedAt(LocalDateTime.now());
        
        batchRepository.save(testBatch);
    }

    @Test
    void validateBulkData_WithInvalidBatch_ShouldThrowException() {
        final BulkImportBatch invalidBatch = new BulkImportBatch();
        invalidBatch.setStatus(null);
        assertThatThrownBy(() -> asyncBulkImportService.validateBulkData(invalidBatch))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Batch is not in PENDING status");
    }

    @Test
    void validateBulkData_WithWrongStatus_ShouldThrowException() {
        testBatch.setStatus(BulkImportBatch.BatchStatus.COMPLETED);
        batchRepository.save(testBatch);
        
        assertThatThrownBy(() -> asyncBulkImportService.validateBulkData(testBatch))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not in PENDING status");
    }

    @Test
    void processBulkImport_WithWrongStatus_ShouldThrowException() {
        testBatch.setStatus(BulkImportBatch.BatchStatus.PENDING);
        batchRepository.save(testBatch);

        assertThatThrownBy(() -> asyncBulkImportService.processBulkImport(testBatch))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be in VALIDATING status");
    }
}
