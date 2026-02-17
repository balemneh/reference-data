package gov.dhs.cbp.reference.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.core.entity.BulkImportBatch;
import gov.dhs.cbp.reference.core.entity.BulkImportStaging;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.BulkImportBatchRepository;
import gov.dhs.cbp.reference.core.repository.BulkImportStagingRepository;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider; // Added import
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation; // Added import

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AsyncBulkImportService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncBulkImportService.class);

    private final BulkImportBatchRepository batchRepository;
    private final BulkImportStagingRepository stagingRepository;
    private final CountryRepository countryRepository;
    private final CodeSystemRepository codeSystemRepository;
    private final ObjectMapper objectMapper;
    private final ChangeRequestService changeRequestService;

    // Use ObjectProvider to break circular dependency and ensure @Transactional proxying for self-invocation
    @Autowired
    private ObjectProvider<AsyncBulkImportService> selfProvider;

    public AsyncBulkImportService(BulkImportBatchRepository batchRepository,
                                  BulkImportStagingRepository stagingRepository,
                                  CountryRepository countryRepository,
                                  CodeSystemRepository codeSystemRepository,
                                  @Autowired(required = false) ObjectMapper objectMapper,
                                  ChangeRequestService changeRequestService) {
        this.batchRepository = batchRepository;
        this.stagingRepository = stagingRepository;
        this.countryRepository = countryRepository;
        this.codeSystemRepository = codeSystemRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.changeRequestService = changeRequestService;
    }

    @Async
    public void validateAndProcess(UUID batchId) {
        // Fetch the batch once for the async operation to ensure it's loaded in this transaction context
        final BulkImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));
        
        // validateBulkData will join this transaction
        selfProvider.getObject().validateBulkData(batch);
        // processBulkImport will run in its own new transaction
        selfProvider.getObject().processBulkImport(batch);
    }

    @Transactional(propagation = Propagation.REQUIRED) // Ensures this runs in its own transaction (as validateAndProcess is no longer transactional)
    public BulkImportService.ValidationSummary validateBulkData(final BulkImportBatch batch) {
        logger.info("Starting validation for batch {}", batch.getId());

        validateBatchStatus(batch, BulkImportBatch.BatchStatus.PENDING, "Batch is not in PENDING status");

        // Update batch status to validating
        batch.setStatus(BulkImportBatch.BatchStatus.VALIDATING);
        batch.setStartedAt(LocalDateTime.now());
        // Removed: batchRepository.save(batch); // Rely on dirty checking and final save

        List<BulkImportStaging> stagingRecords = stagingRepository.findByImportBatchId(batch.getId());
        BulkImportService.ValidationSummary summary = new BulkImportService.ValidationSummary();

        for (BulkImportStaging record : stagingRecords) {
            validateStagingRecord(record, summary);
        }

        // Update batch with validation results
        batch.setRecordsValid(summary.getValidCount());
        batch.setRecordsInvalid(summary.getInvalidCount());
        batch.setRecordsWarnings(summary.getWarningCount());
        batch.updateProgress();
        batchRepository.save(batch); // Keep this final save

        logger.info("Completed validation for batch {} - Valid: {}, Invalid: {}, Warnings: {}",
                   batch.getId(), summary.getValidCount(), summary.getInvalidCount(), summary.getWarningCount());

        return summary;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // Runs in its own new transaction, independent of validateBulkData
    public BulkImportService.ProcessingSummary processBulkImport(final BulkImportBatch batch) {
        logger.info("Starting processing for batch {}", batch.getId());

        validateBatchStatus(batch, BulkImportBatch.BatchStatus.VALIDATING, "Batch must be in VALIDATING status");

        BulkImportService.ProcessingSummary summary = new BulkImportService.ProcessingSummary();

        try {
            // Update batch status to processing
            batch.setStatus(BulkImportBatch.BatchStatus.PROCESSING);
            batch.setStartedAt(LocalDateTime.now()); // Set startedAt here for processing
            // Removed: batchRepository.save(batch); // Rely on dirty checking and final save

            List<BulkImportStaging> validRecords = stagingRepository.findReadyForProcessing(batch.getId());
            
            logger.debug("Processing {} records for batch {}. Current summary: Processed={}, Failed={}",
                         validRecords.size(), batch.getId(), summary.getProcessedCount(), summary.getFailedCount());

            for (BulkImportStaging record : validRecords) {
                logger.debug("Before processing record {}: Summary: Processed={}, Failed={}",
                             record.getId(), summary.getProcessedCount(), summary.getFailedCount());
                try {
                    // Call the new transactional method via the injected proxy
                    selfProvider.getObject().processSingleStagingRecord(record, summary);
                } catch (Exception individualRecordException) {
                    logger.error("Error processing single record {}: {}", record.getId(), individualRecordException.getMessage(), individualRecordException);
                    // Mark the record as failed within the individual record's transaction (handled in processSingleStagingRecord)
                    // Just increment the overall summary's failed count here if an exception somehow escapes
                    // Although processSingleStagingRecord should handle and suppress exceptions, this is a safeguard.
                    summary.incrementFailed();
                }
                logger.debug("After processing record {}: Summary: Processed={}, Failed={}",
                             record.getId(), summary.getProcessedCount(), summary.getFailedCount());
            }

            // Call the new transactional method to save final batch status
            selfProvider.getObject().saveBatchProcessingStatus(batch, summary);

        } catch (Exception e) {
            logger.error("Critical error during bulk import batch processing for batch {}: {}", batch.getId(), e.getMessage(), e);
            // Ensure a general failed count is incremented if the orchestrator itself fails
            summary.incrementFailed(); 
            // Also call saveBatchProcessingStatus to mark the batch as FAILED
            selfProvider.getObject().saveBatchProcessingStatus(batch, summary);
        }

        return summary;
    }
    
    private void validateBatchStatus(BulkImportBatch batch, BulkImportBatch.BatchStatus expectedStatus, String message) {
        if (!expectedStatus.equals(batch.getStatus())) {
            throw new IllegalStateException(message + ", current status: " + batch.getStatus());
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRED) // Joins processBulkImport's transaction
    public void saveBatchProcessingStatus(BulkImportBatch batch, BulkImportService.ProcessingSummary summary) {
        // The batch object is already the latest version (fetched at the beginning of the method).
        logger.info("Batch {} has {} invalid records.", batch.getId(), batch.getRecordsInvalid());

        // Update batch completion
        if (summary.getProcessedCount() > 0 && (summary.getFailedCount() > 0 || batch.getRecordsInvalid() > 0)) {
            batch.setStatus(BulkImportBatch.BatchStatus.PARTIAL);
        } else if (summary.getProcessedCount() == 0 && (summary.getFailedCount() > 0 || batch.getRecordsInvalid() > 0)) {
            batch.setStatus(BulkImportBatch.BatchStatus.FAILED);
        } else {
            batch.setStatus(BulkImportBatch.BatchStatus.COMPLETED);
        }
        batch.setCompletedAt(LocalDateTime.now());
        batch.setRecordsProcessed(summary.getProcessedCount() + summary.getFailedCount());

        if (batch.getStartedAt() != null) {
            long durationSeconds = java.time.Duration.between(batch.getStartedAt(), batch.getCompletedAt()).getSeconds();
            batch.setDurationSeconds((int) durationSeconds);
        }

        batch.updateProgress();
        batchRepository.save(batch); // Save the final status

        logger.info("Completed processing for batch {} - Processed: {}, Failed: {}",
                   batch.getId(), summary.getProcessedCount(), summary.getFailedCount());
    }

    // New public method for processing a single staging record with its own transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = {Exception.class})
    public void processSingleStagingRecord(BulkImportStaging record, BulkImportService.ProcessingSummary summary) {
        logger.debug("Entering processSingleStagingRecord for record {}", record.getId());
        try {
            // Process based on data type
            switch (record.getDataType()) {
                case COUNTRIES:
                    logger.debug("Calling processCountryRecord for record {}", record.getId());
                    processCountryRecord(record);
                    logger.debug("processCountryRecord completed for record {}", record.getId());
                    break;
                default:
                    throw new UnsupportedOperationException("Processing not implemented for data type: " + record.getDataType());
            }

            record.markAsProcessed("SYSTEM");
            stagingRepository.save(record);
            logger.debug("Incrementing processed count for record {}. Current Processed={}, Failed={}",
                         record.getId(), summary.getProcessedCount(), summary.getFailedCount());
            summary.incrementProcessed();

        } catch (Exception e) {
            logger.error("Processing error for record {}: {}", record.getId(), e.getMessage(), e); // Log specific record ID
            record.markAsFailed("Processing error: " + e.getMessage(), "SYSTEM");
            stagingRepository.save(record);
            logger.debug("Incrementing failed count for record {}. Current Processed={}, Failed={}",
                         record.getId(), summary.getProcessedCount(), summary.getFailedCount());
            summary.incrementFailed();
        }
        logger.debug("Exiting processSingleStagingRecord for record {}", record.getId());
    }
    
    private void validateStagingRecord(BulkImportStaging record, BulkImportService.ValidationSummary summary) {
        try {
            Map<String, String> dataMap = objectMapper.readValue(record.getRawData(), Map.class);
            List<gov.dhs.cbp.reference.api.dto.ValidationErrorDto> errors = new ArrayList<>();
            List<gov.dhs.cbp.reference.api.dto.ValidationErrorDto> warnings = new ArrayList<>();

            // Validate based on data type
            switch (record.getDataType()) {
                case COUNTRIES:
                    validateCountryRecord(dataMap, record.getRowNumber(), errors, warnings);
                    break;
                default:
                    // Add validation for other data types as needed
                    break;
            }

            // Update validation status
            if (!errors.isEmpty()) {
                record.markAsInvalid(objectMapper.writeValueAsString(errors.stream().map(gov.dhs.cbp.reference.api.dto.ValidationErrorDto::getMessage).collect(Collectors.toList())), "SYSTEM");
                summary.incrementInvalid();
                errors.forEach(summary::addError);
            } else {
                record.markAsValidated("SYSTEM");
                summary.incrementValid();
                summary.addPreviewData(dataMap);
                if (!warnings.isEmpty()) {
                    record.setValidationWarnings(objectMapper.writeValueAsString(warnings.stream().map(gov.dhs.cbp.reference.api.dto.ValidationErrorDto::getMessage).collect(Collectors.toList())));
                    summary.incrementWarning();
                    warnings.forEach(summary::addWarning);
                }
            }

            stagingRepository.save(record);

        } catch (Exception e) {
            logger.error("Validation error: " + e.getMessage(), e);
            record.markAsInvalid("Validation error: " + e.getMessage(), "SYSTEM");
            summary.incrementInvalid();
            summary.addError(new gov.dhs.cbp.reference.api.dto.ValidationErrorDto(record.getRowNumber(), null, "GENERAL", null, "Validation error: " + e.getMessage(), "ERROR", null));
            stagingRepository.save(record);
        }
    }
    private void validateCountryRecord(Map<String, String> dataMap, int rowNumber, List<gov.dhs.cbp.reference.api.dto.ValidationErrorDto> errors, List<gov.dhs.cbp.reference.api.dto.ValidationErrorDto> warnings) {
        // Required fields validation
        if (dataMap.get("countryCode") == null || dataMap.get("countryCode").trim().isEmpty()) {
            errors.add(new gov.dhs.cbp.reference.api.dto.ValidationErrorDto(rowNumber, "countryCode", "countryCode", dataMap.get("countryCode"), "Country code is required", "ERROR", null));
        }

        if (dataMap.get("countryName") == null || dataMap.get("countryName").trim().isEmpty()) {
            errors.add(new gov.dhs.cbp.reference.api.dto.ValidationErrorDto(rowNumber, "countryName", "countryName", dataMap.get("countryName"), "Country name is required", "ERROR", null));
        } else if (dataMap.get("countryName").length() > 255) {
            errors.add(new gov.dhs.cbp.reference.api.dto.ValidationErrorDto(rowNumber, "countryName", "countryName", dataMap.get("countryName"), "Country name must be 255 characters or less", "ERROR", null));
        }

        // Format validation
        String countryCode = dataMap.get("countryCode");
        if (countryCode != null && countryCode.length() > 10) {
            errors.add(new gov.dhs.cbp.reference.api.dto.ValidationErrorDto(rowNumber, "countryCode", "countryCode", countryCode, "Country code must be 10 characters or less", "ERROR", null));
        }

        String codeSystem = dataMap.get("codeSystem");
        if (codeSystem == null || codeSystem.isBlank()) {
            errors.add(new gov.dhs.cbp.reference.api.dto.ValidationErrorDto(rowNumber, "codeSystem", "codeSystem", codeSystem, "codeSystem is required for Country records.", "ERROR", null));
        }

        String iso2Code = dataMap.get("iso2Code");
        if (iso2Code != null && iso2Code.length() != 2) {
            warnings.add(new gov.dhs.cbp.reference.api.dto.ValidationErrorDto(rowNumber, "iso2Code", "iso2Code", iso2Code, "ISO2 code should be exactly 2 characters", "WARNING", "Ensure ISO2 code is a 2-letter country code."));
        }

        String iso3Code = dataMap.get("iso3Code");
        if (iso3Code != null && iso3Code.length() != 3) {
            warnings.add(new gov.dhs.cbp.reference.api.dto.ValidationErrorDto(rowNumber, "iso3Code", "iso3Code", iso3Code, "ISO3 code should be exactly 3 characters", "WARNING", "Ensure ISO3 code is a 3-letter country code."));
        }

        // Check for existing records - we need code system to check properly
        // For now, just warn that duplicate checking needs code system
        if (countryCode != null) {
            warnings.add(new gov.dhs.cbp.reference.api.dto.ValidationErrorDto(rowNumber, "countryCode", "countryCode", countryCode, "Duplicate checking requires code system information", "WARNING", "Provide a 'codeSystem' field to enable duplicate checking."));
        }
    }

    private void processCountryRecord(BulkImportStaging record) throws Exception {
        logger.debug("Entering processCountryRecord for record {}", record.getId());
        Map<String, String> dataMap = objectMapper.readValue(record.getRawData(), Map.class);

        // Create new Country entity
        Country country = new Country();
        country.setCountryCode(dataMap.get("countryCode"));
        country.setCountryName(dataMap.get("countryName"));
        country.setIso2Code(dataMap.get("iso2Code"));
        country.setIso3Code(dataMap.get("iso3Code"));
        country.setNumericCode(dataMap.get("numericCode"));
        country.setIsActive(Boolean.parseBoolean(dataMap.getOrDefault("isActive", "true")));
        country.setRecordedBy(record.getCreatedBy());
        country.setChangeRequestId(record.getChangeRequestId());

        // Retrieve and set CodeSystem
        String codeSystemName = dataMap.get("codeSystem");
        if (codeSystemName == null || codeSystemName.isBlank()) {
            throw new IllegalArgumentException("codeSystem is required for Country records.");
        }
        gov.dhs.cbp.reference.core.entity.CodeSystem codeSystem = codeSystemRepository.findByCode(codeSystemName)
                .orElseThrow(() -> new IllegalArgumentException("CodeSystem not found: " + codeSystemName));
        country.setCodeSystem(codeSystem);

        // Save the country
        Country savedCountry = countryRepository.save(country);

        // Update staging record with target information
        record.setTargetRecordId(savedCountry.getId());
        record.setTargetVersion(savedCountry.getVersion());
        logger.debug("Exiting processCountryRecord for record {}", record.getId());
    }
}