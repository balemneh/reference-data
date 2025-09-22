package gov.dhs.cbp.reference.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.dto.ChangeRequestDto;
import gov.dhs.cbp.reference.core.entity.BulkImportBatch;
import gov.dhs.cbp.reference.core.entity.BulkImportStaging;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.BulkImportBatchRepository;
import gov.dhs.cbp.reference.core.repository.BulkImportStagingRepository;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkImportServiceTest {

    @Mock
    private BulkImportBatchRepository batchRepository;

    @Mock
    private BulkImportStagingRepository stagingRepository;

    @Mock
    private ChangeRequestService changeRequestService;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BulkImportService bulkImportService;

    private UUID testBatchId;
    private UUID testChangeRequestId;
    private String testUserId;
    private BulkImportBatch testBatch;

    @BeforeEach
    void setUp() {
        testBatchId = UUID.randomUUID();
        testChangeRequestId = UUID.randomUUID();
        testUserId = "test-user";

        testBatch = new BulkImportBatch();
        testBatch.setId(testBatchId);
        testBatch.setBatchName("TEST_BATCH");
        testBatch.setChangeRequestId(testChangeRequestId);
        testBatch.setDataType(BulkImportBatch.DataType.COUNTRIES);
        testBatch.setStatus(BulkImportBatch.BatchStatus.PENDING);
        testBatch.setSourceSystem("TEST_SYSTEM");
        testBatch.setCreatedBy(testUserId);
        testBatch.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void initiateBulkImport_WithValidCsvFile_ShouldSucceed() throws Exception {
        // Arrange
        String csvContent = "countryCode,countryName,iso2Code,iso3Code\nUS,United States,US,USA\nCA,Canada,CA,CAN";
        MockMultipartFile csvFile = new MockMultipartFile(
            "file", "countries.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        ChangeRequestDto mockChangeRequest = new ChangeRequestDto();
        mockChangeRequest.setId(testChangeRequestId);

        when(changeRequestService.create(any(ChangeRequestDto.class))).thenReturn(mockChangeRequest);
        when(batchRepository.findBySourceFileChecksum(anyString())).thenReturn(Optional.empty());

        // Mock batch save to return batch with format set
        when(batchRepository.save(any(BulkImportBatch.class))).thenAnswer(invocation -> {
            BulkImportBatch savedBatch = invocation.getArgument(0);
            if (savedBatch.getSourceFileFormat() == null) {
                savedBatch.setSourceFileFormat("CSV");
            }
            savedBatch.setId(testBatchId);
            return savedBatch;
        });

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
        when(stagingRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        UUID resultBatchId = bulkImportService.initiateBulkImport(
            csvFile, testUserId, BulkImportBatch.DataType.COUNTRIES, "TEST_SYSTEM", "Test import"
        );

        // Assert
        assertThat(resultBatchId).isEqualTo(testBatchId);
        verify(batchRepository, times(2)).save(any(BulkImportBatch.class)); // Once for initial save, once for updating total records
        verify(stagingRepository).saveAll(anyList());
        verify(changeRequestService).create(any(ChangeRequestDto.class));
    }

    @Test
    void initiateBulkImport_WithValidJsonFile_ShouldSucceed() throws Exception {
        // Arrange
        String jsonContent = "[{\"countryCode\":\"US\",\"countryName\":\"United States\",\"iso2Code\":\"US\",\"iso3Code\":\"USA\"}]";
        MockMultipartFile jsonFile = new MockMultipartFile(
            "file", "countries.json", "application/json", jsonContent.getBytes(StandardCharsets.UTF_8)
        );

        ChangeRequestDto mockChangeRequest = new ChangeRequestDto();
        mockChangeRequest.setId(testChangeRequestId);

        when(changeRequestService.create(any(ChangeRequestDto.class))).thenReturn(mockChangeRequest);
        when(batchRepository.findBySourceFileChecksum(anyString())).thenReturn(Optional.empty());

        // Mock batch save to return batch with format set
        when(batchRepository.save(any(BulkImportBatch.class))).thenAnswer(invocation -> {
            BulkImportBatch savedBatch = invocation.getArgument(0);
            if (savedBatch.getSourceFileFormat() == null) {
                savedBatch.setSourceFileFormat("JSON");
            }
            savedBatch.setId(testBatchId);
            return savedBatch;
        });

        when(objectMapper.readTree(any(java.io.InputStream.class))).thenReturn(new ObjectMapper().readTree(jsonContent));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
        when(stagingRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        UUID resultBatchId = bulkImportService.initiateBulkImport(
            jsonFile, testUserId, BulkImportBatch.DataType.COUNTRIES, "TEST_SYSTEM", "Test import"
        );

        // Assert
        assertThat(resultBatchId).isEqualTo(testBatchId);
        verify(batchRepository, times(2)).save(any(BulkImportBatch.class));
        verify(stagingRepository).saveAll(anyList());
    }

    @Test
    void initiateBulkImport_WithDuplicateFile_ShouldThrowException() {
        // Arrange
        String csvContent = "countryCode,countryName\nUS,United States";
        MockMultipartFile csvFile = new MockMultipartFile(
            "file", "countries.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        BulkImportBatch existingBatch = new BulkImportBatch();
        existingBatch.setId(UUID.randomUUID());

        when(batchRepository.findBySourceFileChecksum(anyString())).thenReturn(Optional.of(existingBatch));

        // Act & Assert
        assertThatThrownBy(() -> bulkImportService.initiateBulkImport(
            csvFile, testUserId, BulkImportBatch.DataType.COUNTRIES, "TEST_SYSTEM", "Test import"
        )).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("already been imported");

        verify(batchRepository, never()).save(any(BulkImportBatch.class));
        verify(stagingRepository, never()).saveAll(anyList());
    }

    @Test
    void validateBulkData_WithValidBatch_ShouldSucceed() throws Exception {
        // Arrange
        BulkImportStaging stagingRecord1 = createValidStagingRecord(1);
        BulkImportStaging stagingRecord2 = createValidStagingRecord(2);
        List<BulkImportStaging> stagingRecords = Arrays.asList(stagingRecord1, stagingRecord2);

        when(batchRepository.findById(testBatchId)).thenReturn(Optional.of(testBatch));
        when(stagingRepository.findByImportBatchId(testBatchId)).thenReturn(stagingRecords);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(createValidCountryDataMap());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
        // Removed findCurrentByCode method call as it doesn't exist in repository

        // Act
        BulkImportService.ValidationSummary summary = bulkImportService.validateBulkData(testBatchId);

        // Assert
        assertThat(summary.getValidCount()).isEqualTo(2);
        assertThat(summary.getInvalidCount()).isEqualTo(0);
        assertThat(summary.getWarningCount()).isEqualTo(2); // Each country gets warning about missing code system

        verify(batchRepository).findById(testBatchId);
        verify(stagingRepository).findByImportBatchId(testBatchId);
        verify(batchRepository, times(2)).save(any(BulkImportBatch.class)); // Status update and results update
    }

    @Test
    void validateBulkData_WithInvalidBatch_ShouldThrowException() {
        // Arrange
        when(batchRepository.findById(testBatchId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bulkImportService.validateBulkData(testBatchId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Batch not found");
    }

    @Test
    void validateBulkData_WithWrongStatus_ShouldThrowException() {
        // Arrange
        testBatch.setStatus(BulkImportBatch.BatchStatus.COMPLETED);
        when(batchRepository.findById(testBatchId)).thenReturn(Optional.of(testBatch));

        // Act & Assert
        assertThatThrownBy(() -> bulkImportService.validateBulkData(testBatchId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not in PENDING status");
    }

    @Test
    void processBulkImport_WithValidRecords_ShouldSucceed() throws Exception {
        // Arrange
        testBatch.setStatus(BulkImportBatch.BatchStatus.VALIDATING);
        BulkImportStaging validRecord = createValidStagingRecord(1);
        validRecord.setValidationStatus(BulkImportStaging.ValidationStatus.VALID);
        validRecord.setProcessingStatus(BulkImportStaging.ProcessingStatus.PENDING);

        Country savedCountry = new Country();
        savedCountry.setId(UUID.randomUUID());
        savedCountry.setVersion(1L);

        when(batchRepository.findById(testBatchId)).thenReturn(Optional.of(testBatch));
        when(stagingRepository.findReadyForProcessing(testBatchId)).thenReturn(Collections.singletonList(validRecord));
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(createValidCountryDataMap());
        when(countryRepository.save(any(Country.class))).thenReturn(savedCountry);

        // Act
        BulkImportService.ProcessingSummary summary = bulkImportService.processBulkImport(testBatchId);

        // Assert
        assertThat(summary.getProcessedCount()).isEqualTo(1);
        assertThat(summary.getFailedCount()).isEqualTo(0);

        verify(countryRepository).save(any(Country.class));
        verify(batchRepository, times(2)).save(any(BulkImportBatch.class));
    }

    @Test
    void processBulkImport_WithWrongStatus_ShouldThrowException() {
        // Arrange
        testBatch.setStatus(BulkImportBatch.BatchStatus.PENDING);
        when(batchRepository.findById(testBatchId)).thenReturn(Optional.of(testBatch));

        // Act & Assert
        assertThatThrownBy(() -> bulkImportService.processBulkImport(testBatchId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be in VALIDATING status");
    }

    @Test
    void getBulkImportStatus_WithValidBatch_ShouldReturnStatus() {
        // Arrange
        testBatch.setTotalRecords(100);
        testBatch.setRecordsProcessed(50);
        testBatch.setRecordsValid(80);
        testBatch.setRecordsInvalid(10);
        testBatch.setRecordsWarnings(5);

        when(batchRepository.findById(testBatchId)).thenReturn(Optional.of(testBatch));

        // Act
        BulkImportService.BulkImportStatus status = bulkImportService.getBulkImportStatus(testBatchId);

        // Assert
        assertThat(status.getBatchId()).isEqualTo(testBatchId);
        assertThat(status.getBatchName()).isEqualTo("TEST_BATCH");
        assertThat(status.getStatus()).isEqualTo(BulkImportBatch.BatchStatus.PENDING);
        assertThat(status.getTotalRecords()).isEqualTo(100);
        assertThat(status.getRecordsProcessed()).isEqualTo(50);
        assertThat(status.getRecordsValid()).isEqualTo(80);
        assertThat(status.getRecordsInvalid()).isEqualTo(10);
        assertThat(status.getRecordsWarnings()).isEqualTo(5);
        assertThat(status.getProgressPercentage()).isEqualTo(50.0);
    }

    @Test
    void getBulkImportStatus_WithInvalidBatch_ShouldThrowException() {
        // Arrange
        when(batchRepository.findById(testBatchId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bulkImportService.getBulkImportStatus(testBatchId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Batch not found");
    }

    @Test
    void rollbackBulkImport_WithValidBatch_ShouldSucceed() {
        // Arrange
        testBatch.setStatus(BulkImportBatch.BatchStatus.FAILED);
        BulkImportStaging processedRecord = createValidStagingRecord(1);
        processedRecord.setProcessingStatus(BulkImportStaging.ProcessingStatus.COMPLETED);

        when(batchRepository.findById(testBatchId)).thenReturn(Optional.of(testBatch));
        when(stagingRepository.findByBatchIdAndProcessingStatus(testBatchId, BulkImportStaging.ProcessingStatus.COMPLETED))
            .thenReturn(Collections.singletonList(processedRecord));

        // Act
        boolean result = bulkImportService.rollbackBulkImport(testBatchId);

        // Assert
        assertThat(result).isTrue();
        verify(stagingRepository).save(any(BulkImportStaging.class));
        verify(batchRepository).save(any(BulkImportBatch.class));
    }

    @Test
    void rollbackBulkImport_WithCompletedBatch_ShouldThrowException() {
        // Arrange
        testBatch.setStatus(BulkImportBatch.BatchStatus.COMPLETED);
        when(batchRepository.findById(testBatchId)).thenReturn(Optional.of(testBatch));

        // Act & Assert
        assertThatThrownBy(() -> bulkImportService.rollbackBulkImport(testBatchId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot rollback a completed batch");
    }

    @Test
    void initiateBulkImport_WithEmptyFile_ShouldHandleGracefully() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "empty.csv", "text/csv", "".getBytes(StandardCharsets.UTF_8)
        );

        ChangeRequestDto mockChangeRequest = new ChangeRequestDto();
        mockChangeRequest.setId(testChangeRequestId);

        when(changeRequestService.create(any(ChangeRequestDto.class))).thenReturn(mockChangeRequest);
        when(batchRepository.findBySourceFileChecksum(anyString())).thenReturn(Optional.empty());
        when(batchRepository.save(any(BulkImportBatch.class))).thenReturn(testBatch);

        // Act & Assert
        assertThatThrownBy(() -> bulkImportService.initiateBulkImport(
            emptyFile, testUserId, BulkImportBatch.DataType.COUNTRIES, "TEST_SYSTEM", "Test import"
        )).isInstanceOf(RuntimeException.class);
    }

    @Test
    void validateBulkData_WithInvalidCountryData_ShouldMarkAsInvalid() throws Exception {
        // Arrange
        BulkImportStaging stagingRecord = createInvalidStagingRecord();
        Map<String, String> invalidData = new HashMap<>();
        invalidData.put("countryCode", ""); // Empty country code
        invalidData.put("countryName", "");  // Empty country name

        when(batchRepository.findById(testBatchId)).thenReturn(Optional.of(testBatch));
        when(stagingRepository.findByImportBatchId(testBatchId)).thenReturn(Collections.singletonList(stagingRecord));
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(invalidData);
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"Country code is required\"]");

        // Act
        BulkImportService.ValidationSummary summary = bulkImportService.validateBulkData(testBatchId);

        // Assert
        assertThat(summary.getValidCount()).isEqualTo(0);
        assertThat(summary.getInvalidCount()).isEqualTo(1);
        assertThat(summary.getWarningCount()).isEqualTo(0);
    }

    // Helper methods
    private BulkImportStaging createValidStagingRecord(int rowNumber) {
        BulkImportStaging staging = new BulkImportStaging();
        staging.setId(UUID.randomUUID());
        staging.setImportBatchId(testBatchId);
        staging.setChangeRequestId(testChangeRequestId);
        staging.setDataType(BulkImportStaging.DataType.COUNTRIES);
        staging.setOperationType(BulkImportStaging.OperationType.UPSERT);
        staging.setRowNumber(rowNumber);
        staging.setNaturalKey("TEST_COUNTRY_" + rowNumber);
        staging.setTargetTable("countries_v");
        staging.setRawData("{\"countryCode\":\"T" + rowNumber + "\",\"countryName\":\"Test Country " + rowNumber + "\"}");
        staging.setValidationStatus(BulkImportStaging.ValidationStatus.PENDING);
        staging.setProcessingStatus(BulkImportStaging.ProcessingStatus.PENDING);
        staging.setCreatedBy(testUserId);
        staging.setUpdatedBy(testUserId);
        return staging;
    }

    private BulkImportStaging createInvalidStagingRecord() {
        BulkImportStaging staging = new BulkImportStaging();
        staging.setId(UUID.randomUUID());
        staging.setImportBatchId(testBatchId);
        staging.setChangeRequestId(testChangeRequestId);
        staging.setDataType(BulkImportStaging.DataType.COUNTRIES);
        staging.setOperationType(BulkImportStaging.OperationType.UPSERT);
        staging.setRowNumber(1);
        staging.setNaturalKey("INVALID_COUNTRY");
        staging.setTargetTable("countries_v");
        staging.setRawData("{\"countryCode\":\"\",\"countryName\":\"\"}");
        staging.setValidationStatus(BulkImportStaging.ValidationStatus.PENDING);
        staging.setProcessingStatus(BulkImportStaging.ProcessingStatus.PENDING);
        staging.setCreatedBy(testUserId);
        staging.setUpdatedBy(testUserId);
        return staging;
    }

    private Map<String, String> createValidCountryDataMap() {
        Map<String, String> data = new HashMap<>();
        data.put("countryCode", "US");
        data.put("countryName", "United States");
        data.put("iso2Code", "US");
        data.put("iso3Code", "USA");
        data.put("numericCode", "840");
        data.put("isActive", "true");
        return data;
    }
}