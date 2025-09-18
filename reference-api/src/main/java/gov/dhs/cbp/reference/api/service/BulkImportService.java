package gov.dhs.cbp.reference.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.dto.ChangeRequestDto;
import gov.dhs.cbp.reference.core.entity.BulkImportBatch;
import gov.dhs.cbp.reference.core.entity.BulkImportStaging;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.BulkImportBatchRepository;
import gov.dhs.cbp.reference.core.repository.BulkImportStagingRepository;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BulkImportService {

    private static final Logger logger = LoggerFactory.getLogger(BulkImportService.class);

    private final BulkImportBatchRepository batchRepository;
    private final BulkImportStagingRepository stagingRepository;
    private final ChangeRequestService changeRequestService;
    private final CountryRepository countryRepository;
    private final ObjectMapper objectMapper;

    public BulkImportService(BulkImportBatchRepository batchRepository,
                            BulkImportStagingRepository stagingRepository,
                            ChangeRequestService changeRequestService,
                            CountryRepository countryRepository,
                            @Autowired(required = false) ObjectMapper objectMapper) {
        this.batchRepository = batchRepository;
        this.stagingRepository = stagingRepository;
        this.changeRequestService = changeRequestService;
        this.countryRepository = countryRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Initiates a bulk import process
     *
     * @param file The file to import (CSV or JSON)
     * @param userId The user initiating the import
     * @param dataType The type of data being imported
     * @param sourceSystem The source system name
     * @param description Optional description of the import
     * @return The created batch ID
     */
    @Transactional
    public UUID initiateBulkImport(MultipartFile file, String userId, BulkImportBatch.DataType dataType,
                                  String sourceSystem, String description) {
        logger.info("Initiating bulk import for user {} with file {} for data type {}",
                   userId, file.getOriginalFilename(), dataType);

        try {
            // Calculate file checksum
            String checksum = calculateFileChecksum(file.getBytes());

            // Check for duplicate imports
            Optional<BulkImportBatch> existingBatch = batchRepository.findBySourceFileChecksum(checksum);
            if (existingBatch.isPresent()) {
                logger.warn("Duplicate file detected with checksum {}, existing batch {}",
                           checksum, existingBatch.get().getId());
                throw new IllegalArgumentException("This file has already been imported. Batch ID: " + existingBatch.get().getId());
            }

            // Create change request for the bulk import
            ChangeRequestDto changeRequest = createChangeRequestForBulkImport(userId, dataType, file.getOriginalFilename(), description);

            // Create bulk import batch
            BulkImportBatch batch = new BulkImportBatch();
            batch.setBatchName(generateBatchName(dataType, file.getOriginalFilename()));
            batch.setDescription(description);
            batch.setChangeRequestId(changeRequest.getId());
            batch.setSourceSystem(sourceSystem);
            batch.setSourceFileName(file.getOriginalFilename());
            batch.setSourceFileSize(file.getSize());
            batch.setSourceFileChecksum(checksum);
            batch.setSourceFileFormat(detectFileFormat(file.getOriginalFilename()));
            batch.setDataType(dataType);
            batch.setImportMode(BulkImportBatch.ImportMode.STANDARD);
            batch.setStatus(BulkImportBatch.BatchStatus.PENDING);
            batch.setCreatedBy(userId);

            batch = batchRepository.save(batch);

            // Parse and stage the file data
            parseAndStageFile(file, batch, userId);

            logger.info("Successfully initiated bulk import batch {}", batch.getId());
            return batch.getId();

        } catch (Exception e) {
            logger.error("Failed to initiate bulk import", e);
            throw new RuntimeException("Failed to initiate bulk import: " + e.getMessage(), e);
        }
    }

    /**
     * Validates all staged records in a batch
     *
     * @param batchId The batch ID to validate
     * @return Validation summary
     */
    @Transactional
    public ValidationSummary validateBulkData(UUID batchId) {
        logger.info("Starting validation for batch {}", batchId);

        BulkImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        if (!BulkImportBatch.BatchStatus.PENDING.equals(batch.getStatus())) {
            throw new IllegalStateException("Batch is not in PENDING status, current status: " + batch.getStatus());
        }

        // Update batch status to validating
        batch.setStatus(BulkImportBatch.BatchStatus.VALIDATING);
        batch.setStartedAt(LocalDateTime.now());
        batchRepository.save(batch);

        List<BulkImportStaging> stagingRecords = stagingRepository.findByImportBatchId(batchId);
        ValidationSummary summary = new ValidationSummary();

        for (BulkImportStaging record : stagingRecords) {
            validateStagingRecord(record, summary);
        }

        // Update batch with validation results
        batch.setRecordsValid(summary.getValidCount());
        batch.setRecordsInvalid(summary.getInvalidCount());
        batch.setRecordsWarnings(summary.getWarningCount());
        batch.updateProgress();
        batchRepository.save(batch);

        logger.info("Completed validation for batch {} - Valid: {}, Invalid: {}, Warnings: {}",
                   batchId, summary.getValidCount(), summary.getInvalidCount(), summary.getWarningCount());

        return summary;
    }

    /**
     * Processes all valid records in a batch
     *
     * @param batchId The batch ID to process
     * @return Processing summary
     */
    @Transactional
    public ProcessingSummary processBulkImport(UUID batchId) {
        logger.info("Starting processing for batch {}", batchId);

        BulkImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        if (!BulkImportBatch.BatchStatus.VALIDATING.equals(batch.getStatus())) {
            throw new IllegalStateException("Batch must be in VALIDATING status, current status: " + batch.getStatus());
        }

        // Update batch status to processing
        batch.setStatus(BulkImportBatch.BatchStatus.PROCESSING);
        batchRepository.save(batch);

        List<BulkImportStaging> validRecords = stagingRepository.findReadyForProcessing(batchId);
        ProcessingSummary summary = new ProcessingSummary();

        for (BulkImportStaging record : validRecords) {
            processStagingRecord(record, summary);
        }

        // Update batch completion
        batch.setStatus(summary.getFailedCount() > 0 ? BulkImportBatch.BatchStatus.FAILED : BulkImportBatch.BatchStatus.COMPLETED);
        batch.setCompletedAt(LocalDateTime.now());
        batch.setRecordsProcessed(summary.getProcessedCount());

        if (batch.getStartedAt() != null) {
            long durationSeconds = java.time.Duration.between(batch.getStartedAt(), batch.getCompletedAt()).getSeconds();
            batch.setDurationSeconds((int) durationSeconds);
        }

        batch.updateProgress();
        batchRepository.save(batch);

        logger.info("Completed processing for batch {} - Processed: {}, Failed: {}",
                   batchId, summary.getProcessedCount(), summary.getFailedCount());

        return summary;
    }

    /**
     * Gets the status of a bulk import batch
     *
     * @param batchId The batch ID
     * @return Batch status information
     */
    public BulkImportStatus getBulkImportStatus(UUID batchId) {
        BulkImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        BulkImportStatus status = new BulkImportStatus();
        status.setBatchId(batchId);
        status.setBatchName(batch.getBatchName());
        status.setStatus(batch.getStatus());
        status.setTotalRecords(batch.getTotalRecords());
        status.setRecordsProcessed(batch.getRecordsProcessed());
        status.setRecordsValid(batch.getRecordsValid());
        status.setRecordsInvalid(batch.getRecordsInvalid());
        status.setRecordsWarnings(batch.getRecordsWarnings());
        status.setRecordsSkipped(batch.getRecordsSkipped());
        status.setProgressPercentage(batch.getProgressPercentage());
        status.setCreatedAt(batch.getCreatedAt());
        status.setStartedAt(batch.getStartedAt());
        status.setCompletedAt(batch.getCompletedAt());
        status.setCreatedBy(batch.getCreatedBy());

        // Get error summary if available
        if (batch.getErrorSummary() != null) {
            try {
                status.setErrors(objectMapper.readTree(batch.getErrorSummary()));
            } catch (JsonProcessingException e) {
                logger.warn("Failed to parse error summary for batch {}", batchId, e);
            }
        }

        return status;
    }

    /**
     * Rolls back a failed bulk import
     *
     * @param batchId The batch ID to rollback
     * @return True if rollback was successful
     */
    @Transactional
    public boolean rollbackBulkImport(UUID batchId) {
        logger.info("Starting rollback for batch {}", batchId);

        BulkImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        if (BulkImportBatch.BatchStatus.COMPLETED.equals(batch.getStatus())) {
            throw new IllegalStateException("Cannot rollback a completed batch");
        }

        try {
            // Find all processed staging records that created actual records
            List<BulkImportStaging> processedRecords = stagingRepository
                    .findByBatchIdAndProcessingStatus(batchId, BulkImportStaging.ProcessingStatus.COMPLETED);

            // For each processed record, we need to reverse the changes
            // This is complex for bitemporal data - we'd need to mark records as invalid
            // For now, we'll just mark the batch as cancelled and the staging records as skipped

            for (BulkImportStaging record : processedRecords) {
                record.setProcessingStatus(BulkImportStaging.ProcessingStatus.SKIPPED);
                record.setProcessingErrors("{\"rollback\": \"Record processing was rolled back\"}");
                stagingRepository.save(record);
            }

            // Update batch status
            batch.setStatus(BulkImportBatch.BatchStatus.CANCELLED);
            batch.setCompletedAt(LocalDateTime.now());
            batchRepository.save(batch);

            logger.info("Successfully rolled back batch {}", batchId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to rollback batch {}", batchId, e);
            return false;
        }
    }

    // Private helper methods

    private String calculateFileChecksum(byte[] fileBytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(fileBytes);
        return Base64.getEncoder().encodeToString(hash);
    }

    private ChangeRequestDto createChangeRequestForBulkImport(String userId, BulkImportBatch.DataType dataType,
                                                             String fileName, String description) {
        ChangeRequestDto changeRequest = new ChangeRequestDto();
        changeRequest.setTitle("Bulk Import: " + fileName);
        changeRequest.setOperationType("CREATE"); // Bulk imports typically create new records
        changeRequest.setEntityType(dataType.name());
        changeRequest.setRequestor(userId);
        changeRequest.setPriority(2); // MEDIUM priority
        changeRequest.setJustification("Bulk data import from " + fileName);

        return changeRequestService.create(changeRequest);
    }

    private String generateBatchName(BulkImportBatch.DataType dataType, String fileName) {
        String timestamp = LocalDateTime.now().toString().replaceAll(":", "-").substring(0, 19);
        return String.format("BULK_%s_%s_%s", dataType.name(), fileName.replaceAll("[^a-zA-Z0-9.]", "_"), timestamp);
    }

    private String detectFileFormat(String fileName) {
        if (fileName == null) return "UNKNOWN";

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "csv": return "CSV";
            case "json": return "JSON";
            case "xlsx": return "EXCEL";
            case "xml": return "XML";
            default: return "UNKNOWN";
        }
    }

    private void parseAndStageFile(MultipartFile file, BulkImportBatch batch, String userId) throws IOException {
        String format = batch.getSourceFileFormat();

        switch (format) {
            case "CSV":
                parseCsvFile(file, batch, userId);
                break;
            case "JSON":
                parseJsonFile(file, batch, userId);
                break;
            default:
                throw new IllegalArgumentException("Unsupported file format: " + format);
        }
    }

    private void parseCsvFile(MultipartFile file, BulkImportBatch batch, String userId) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            String[] headers = headerLine.split(",");
            AtomicInteger rowNumber = new AtomicInteger(1); // Start from 1, header is row 0
            List<BulkImportStaging> stagingRecords = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                rowNumber.incrementAndGet();
                try {
                    BulkImportStaging stagingRecord = parseCsvLine(line, headers, batch, rowNumber.get(), userId);
                    stagingRecords.add(stagingRecord);

                    // Save in batches to avoid memory issues
                    if (stagingRecords.size() >= 100) {
                        stagingRepository.saveAll(stagingRecords);
                        stagingRecords.clear();
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse CSV line {} in batch {}: {}", rowNumber.get(), batch.getId(), e.getMessage());
                    // Create a staging record for the failed parse
                    BulkImportStaging errorRecord = createErrorStagingRecord(line, batch, rowNumber.get(), userId,
                                                                            "Parse error: " + e.getMessage());
                    stagingRecords.add(errorRecord);
                }
            }

            // Save remaining records
            if (!stagingRecords.isEmpty()) {
                stagingRepository.saveAll(stagingRecords);
            }

            // Update batch with total record count
            batch.setTotalRecords(rowNumber.get() - 1); // Subtract 1 for header
            batchRepository.save(batch);
        }
    }

    private void parseJsonFile(MultipartFile file, BulkImportBatch batch, String userId) throws IOException {
        JsonNode rootNode = objectMapper.readTree(file.getInputStream());

        if (!rootNode.isArray()) {
            throw new IllegalArgumentException("JSON file must contain an array of records");
        }

        AtomicInteger rowNumber = new AtomicInteger(0);
        List<BulkImportStaging> stagingRecords = new ArrayList<>();

        for (JsonNode recordNode : rootNode) {
            rowNumber.incrementAndGet();
            try {
                BulkImportStaging stagingRecord = parseJsonRecord(recordNode, batch, rowNumber.get(), userId);
                stagingRecords.add(stagingRecord);

                // Save in batches
                if (stagingRecords.size() >= 100) {
                    stagingRepository.saveAll(stagingRecords);
                    stagingRecords.clear();
                }
            } catch (Exception e) {
                logger.warn("Failed to parse JSON record {} in batch {}: {}", rowNumber.get(), batch.getId(), e.getMessage());
                BulkImportStaging errorRecord = createErrorStagingRecord(recordNode.toString(), batch, rowNumber.get(), userId,
                                                                        "Parse error: " + e.getMessage());
                stagingRecords.add(errorRecord);
            }
        }

        // Save remaining records
        if (!stagingRecords.isEmpty()) {
            stagingRepository.saveAll(stagingRecords);
        }

        batch.setTotalRecords(rowNumber.get());
        batchRepository.save(batch);
    }

    private BulkImportStaging parseCsvLine(String line, String[] headers, BulkImportBatch batch, int rowNumber, String userId) {
        String[] values = line.split(",");

        // Create JSON object from CSV data
        Map<String, String> dataMap = new HashMap<>();
        for (int i = 0; i < Math.min(headers.length, values.length); i++) {
            dataMap.put(headers[i].trim(), values[i].trim());
        }

        return createStagingRecord(dataMap, batch, rowNumber, userId);
    }

    private BulkImportStaging parseJsonRecord(JsonNode recordNode, BulkImportBatch batch, int rowNumber, String userId) throws JsonProcessingException {
        Map<String, String> dataMap = new HashMap<>();
        recordNode.fields().forEachRemaining(entry -> {
            dataMap.put(entry.getKey(), entry.getValue().asText());
        });

        return createStagingRecord(dataMap, batch, rowNumber, userId);
    }

    private BulkImportStaging createStagingRecord(Map<String, String> dataMap, BulkImportBatch batch, int rowNumber, String userId) {
        BulkImportStaging staging = new BulkImportStaging();
        staging.setImportBatchId(batch.getId());
        staging.setChangeRequestId(batch.getChangeRequestId());
        staging.setDataType(mapToStagingDataType(batch.getDataType()));
        staging.setOperationType(BulkImportStaging.OperationType.UPSERT);
        staging.setSourceSystem(batch.getSourceSystem());
        staging.setSourceFileName(batch.getSourceFileName());
        staging.setSourceFileChecksum(batch.getSourceFileChecksum());
        staging.setRowNumber(rowNumber);
        staging.setTargetTable(getTargetTableName(batch.getDataType()));
        staging.setCreatedBy(userId);
        staging.setUpdatedBy(userId);

        // Set natural key based on data type
        staging.setNaturalKey(extractNaturalKey(dataMap, batch.getDataType()));

        // Store raw data as JSON
        try {
            staging.setRawData(objectMapper.writeValueAsString(dataMap));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize raw data", e);
        }

        return staging;
    }

    private BulkImportStaging createErrorStagingRecord(String rawData, BulkImportBatch batch, int rowNumber, String userId, String error) {
        BulkImportStaging staging = new BulkImportStaging();
        staging.setImportBatchId(batch.getId());
        staging.setChangeRequestId(batch.getChangeRequestId());
        staging.setDataType(mapToStagingDataType(batch.getDataType()));
        staging.setOperationType(BulkImportStaging.OperationType.UPSERT);
        staging.setSourceSystem(batch.getSourceSystem());
        staging.setSourceFileName(batch.getSourceFileName());
        staging.setRowNumber(rowNumber);
        staging.setNaturalKey("ERROR_ROW_" + rowNumber);
        staging.setTargetTable(getTargetTableName(batch.getDataType()));
        staging.setRawData(rawData);
        staging.setValidationStatus(BulkImportStaging.ValidationStatus.INVALID);
        staging.setValidationErrors("{\"error\": \"" + error + "\"}");
        staging.setCreatedBy(userId);
        staging.setUpdatedBy(userId);

        return staging;
    }

    private BulkImportStaging.DataType mapToStagingDataType(BulkImportBatch.DataType batchDataType) {
        return BulkImportStaging.DataType.valueOf(batchDataType.name());
    }

    private String getTargetTableName(BulkImportBatch.DataType dataType) {
        switch (dataType) {
            case COUNTRIES: return "countries_v";
            case AIRPORTS: return "airports_v";
            case PORTS: return "ports_v";
            case CARRIERS: return "carriers_v";
            case LANGUAGES: return "languages_v";
            case CURRENCIES: return "currencies_v";
            case UNITS: return "units_v";
            case CODE_MAPPINGS: return "code_mapping";
            default: throw new IllegalArgumentException("Unknown data type: " + dataType);
        }
    }

    private String extractNaturalKey(Map<String, String> dataMap, BulkImportBatch.DataType dataType) {
        switch (dataType) {
            case COUNTRIES:
                // For countries, use country code + code system
                String countryCode = dataMap.get("countryCode");
                String codeSystem = dataMap.get("codeSystem");
                if (countryCode != null && codeSystem != null) {
                    return codeSystem + ":" + countryCode;
                }
                return dataMap.get("countryCode");
            default:
                // For other types, use the first available identifier
                return dataMap.values().iterator().next();
        }
    }

    private void validateStagingRecord(BulkImportStaging record, ValidationSummary summary) {
        try {
            Map<String, String> dataMap = objectMapper.readValue(record.getRawData(), Map.class);
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // Validate based on data type
            switch (record.getDataType()) {
                case COUNTRIES:
                    validateCountryRecord(dataMap, errors, warnings);
                    break;
                default:
                    // Add validation for other data types as needed
                    break;
            }

            // Update validation status
            if (!errors.isEmpty()) {
                record.markAsInvalid(objectMapper.writeValueAsString(errors), "SYSTEM");
                summary.incrementInvalid();
            } else {
                record.markAsValidated("SYSTEM");
                summary.incrementValid();
                if (!warnings.isEmpty()) {
                    record.setValidationWarnings(objectMapper.writeValueAsString(warnings));
                    summary.incrementWarning();
                }
            }

            stagingRepository.save(record);

        } catch (Exception e) {
            record.markAsInvalid("Validation error: " + e.getMessage(), "SYSTEM");
            summary.incrementInvalid();
            stagingRepository.save(record);
        }
    }

    private void validateCountryRecord(Map<String, String> dataMap, List<String> errors, List<String> warnings) {
        // Required fields validation
        if (dataMap.get("countryCode") == null || dataMap.get("countryCode").trim().isEmpty()) {
            errors.add("Country code is required");
        }

        if (dataMap.get("countryName") == null || dataMap.get("countryName").trim().isEmpty()) {
            errors.add("Country name is required");
        }

        // Format validation
        String countryCode = dataMap.get("countryCode");
        if (countryCode != null && countryCode.length() > 10) {
            errors.add("Country code must be 10 characters or less");
        }

        String iso2Code = dataMap.get("iso2Code");
        if (iso2Code != null && iso2Code.length() != 2) {
            warnings.add("ISO2 code should be exactly 2 characters");
        }

        String iso3Code = dataMap.get("iso3Code");
        if (iso3Code != null && iso3Code.length() != 3) {
            warnings.add("ISO3 code should be exactly 3 characters");
        }

        // Check for existing records - we need code system to check properly
        // For now, just warn that duplicate checking needs code system
        if (countryCode != null) {
            warnings.add("Duplicate checking requires code system information");
        }
    }

    private void processStagingRecord(BulkImportStaging record, ProcessingSummary summary) {
        try {
            // Process based on data type
            switch (record.getDataType()) {
                case COUNTRIES:
                    processCountryRecord(record);
                    break;
                default:
                    throw new UnsupportedOperationException("Processing not implemented for data type: " + record.getDataType());
            }

            record.markAsProcessed("SYSTEM");
            stagingRepository.save(record);
            summary.incrementProcessed();

        } catch (Exception e) {
            record.markAsFailed("Processing error: " + e.getMessage(), "SYSTEM");
            stagingRepository.save(record);
            summary.incrementFailed();
        }
    }

    private void processCountryRecord(BulkImportStaging record) throws Exception {
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
        country.setChangeRequestId(record.getChangeRequestId().toString());

        // Save the country
        Country savedCountry = countryRepository.save(country);

        // Update staging record with target information
        record.setTargetRecordId(savedCountry.getId());
        record.setTargetVersion(savedCountry.getVersion());
    }

    // Status and summary classes
    public static class BulkImportStatus {
        private UUID batchId;
        private String batchName;
        private BulkImportBatch.BatchStatus status;
        private Integer totalRecords;
        private Integer recordsProcessed;
        private Integer recordsValid;
        private Integer recordsInvalid;
        private Integer recordsWarnings;
        private Integer recordsSkipped;
        private double progressPercentage;
        private LocalDateTime createdAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String createdBy;
        private JsonNode errors;

        // Getters and setters
        public UUID getBatchId() { return batchId; }
        public void setBatchId(UUID batchId) { this.batchId = batchId; }
        public String getBatchName() { return batchName; }
        public void setBatchName(String batchName) { this.batchName = batchName; }
        public BulkImportBatch.BatchStatus getStatus() { return status; }
        public void setStatus(BulkImportBatch.BatchStatus status) { this.status = status; }
        public Integer getTotalRecords() { return totalRecords; }
        public void setTotalRecords(Integer totalRecords) { this.totalRecords = totalRecords; }
        public Integer getRecordsProcessed() { return recordsProcessed; }
        public void setRecordsProcessed(Integer recordsProcessed) { this.recordsProcessed = recordsProcessed; }
        public Integer getRecordsValid() { return recordsValid; }
        public void setRecordsValid(Integer recordsValid) { this.recordsValid = recordsValid; }
        public Integer getRecordsInvalid() { return recordsInvalid; }
        public void setRecordsInvalid(Integer recordsInvalid) { this.recordsInvalid = recordsInvalid; }
        public Integer getRecordsWarnings() { return recordsWarnings; }
        public void setRecordsWarnings(Integer recordsWarnings) { this.recordsWarnings = recordsWarnings; }
        public Integer getRecordsSkipped() { return recordsSkipped; }
        public void setRecordsSkipped(Integer recordsSkipped) { this.recordsSkipped = recordsSkipped; }
        public double getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(double progressPercentage) { this.progressPercentage = progressPercentage; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        public JsonNode getErrors() { return errors; }
        public void setErrors(JsonNode errors) { this.errors = errors; }
    }

    public static class ValidationSummary {
        private int validCount = 0;
        private int invalidCount = 0;
        private int warningCount = 0;

        public void incrementValid() { validCount++; }
        public void incrementInvalid() { invalidCount++; }
        public void incrementWarning() { warningCount++; }

        public int getValidCount() { return validCount; }
        public int getInvalidCount() { return invalidCount; }
        public int getWarningCount() { return warningCount; }
    }

    public static class ProcessingSummary {
        private int processedCount = 0;
        private int failedCount = 0;

        public void incrementProcessed() { processedCount++; }
        public void incrementFailed() { failedCount++; }

        public int getProcessedCount() { return processedCount; }
        public int getFailedCount() { return failedCount; }
    }
}