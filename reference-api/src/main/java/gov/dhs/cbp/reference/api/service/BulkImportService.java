package gov.dhs.cbp.reference.api.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.dto.ChangeRequestDto;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private final CodeSystemRepository codeSystemRepository;
    private final ObjectMapper objectMapper;
    private final AsyncBulkImportService asyncBulkImportService;

    public BulkImportService(BulkImportBatchRepository batchRepository,
                            BulkImportStagingRepository stagingRepository,
                            ChangeRequestService changeRequestService,
                            CountryRepository countryRepository,
                            CodeSystemRepository codeSystemRepository,
                            @Autowired(required = false) ObjectMapper objectMapper,
                            AsyncBulkImportService asyncBulkImportService) {
        this.batchRepository = batchRepository;
        this.stagingRepository = stagingRepository;
        this.changeRequestService = changeRequestService;
        this.countryRepository = countryRepository;
        this.codeSystemRepository = codeSystemRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.asyncBulkImportService = asyncBulkImportService;
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
    public Map<String, UUID> initiateBulkImport(MultipartFile file, String userId, BulkImportBatch.DataType dataType,
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
            batch.setSourceFile(file.getBytes());
            batch.setDataType(dataType);
            batch.setImportMode(BulkImportBatch.ImportMode.STANDARD);
            batch.setStatus(BulkImportBatch.BatchStatus.PENDING);
            batch.setCreatedBy(userId);

            batch = batchRepository.save(batch);

            // Parse and stage the file data
            parseAndStageFile(file, batch, userId);

            // Asynchronously validate and process the data, but only AFTER the current transaction commits
            final BulkImportBatch savedBatch = batch;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    asyncBulkImportService.validateAndProcess(savedBatch.getId());
                }
            });

            logger.info("Successfully initiated bulk import batch {}", batch.getId());
            Map<String, UUID> ids = new HashMap<>();
            ids.put("batchId", batch.getId());
            ids.put("changeRequestId", changeRequest.getId());
            return ids;

        } catch (Exception e) {
            logger.error("Failed to initiate bulk import", e);
            throw new RuntimeException("Failed to initiate bulk import: " + e.getMessage(), e);
        }
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
        changeRequest.setOperationType("BULK_IMPORT"); // Bulk imports typically create new records
        changeRequest.setEntityType(dataType.name());
        changeRequest.setRequestedBy(userId);
        changeRequest.setPriority(2); // MEDIUM priority
        changeRequest.setBusinessJustification("Bulk data import from " + fileName);

        return changeRequestService.create(changeRequest);
    }

    public Resource getImportedFile(UUID batchId) {
        BulkImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        if (batch.getSourceFile() == null) {
            throw new IllegalArgumentException("File not found for batch: " + batchId);
        }

        return new ByteArrayResource(batch.getSourceFile());
    }

    private String generateBatchName(BulkImportBatch.DataType dataType, String fileName) {
        String timestamp = LocalDateTime.now().toString().replaceAll(":", "-").substring(0, 19);
        return String.format("BULK_%s_%s_%s", dataType.name(), fileName.replaceAll("[^a-zA-Z0-9.]", "_"), timestamp);
    }

    private String detectFileFormat(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "UNKNOWN";

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "UNKNOWN";
        }

        String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
        if (extension == null || extension.isEmpty()) {
            return "UNKNOWN";
        }

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

        if (format == null) {
            format = "UNKNOWN";
        }

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
                if (line.trim().isEmpty() || line.trim().matches("[,\\s]*")) {
                    logger.debug("Skipping blank or comma-only line in CSV: {}", line);
                    continue;
                }
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
        // Validate field lengths before creating the entity
        String naturalKey = extractNaturalKey(dataMap, batch.getDataType());
        if (naturalKey == null || naturalKey.length() > 255) {
            throw new IllegalArgumentException("Natural key must be non-null and less than 255 characters.");
        }

        if (userId == null || userId.length() > 100) {
            throw new IllegalArgumentException("User ID must be non-null and less than 100 characters.");
        }
        
        if (batch.getSourceSystem() == null || batch.getSourceSystem().length() > 100) {
            throw new IllegalArgumentException("Source system must be non-null and less than 100 characters.");
        }
        
        String targetTable = getTargetTableName(batch.getDataType());
        if (targetTable == null || targetTable.length() > 100) {
            throw new IllegalArgumentException("Target table must be non-null and less than 100 characters.");
        }
        
        if (batch.getSourceFileName() != null && batch.getSourceFileName().length() > 500) {
            throw new IllegalArgumentException("Source file name must be less than 500 characters.");
        }
        
        if (batch.getSourceFileChecksum() != null && batch.getSourceFileChecksum().length() > 64) {
            throw new IllegalArgumentException("Source file checksum must be less than 64 characters.");
        }
        
        BulkImportStaging staging = new BulkImportStaging();
        staging.setImportBatchId(batch.getId());
        staging.setChangeRequestId(batch.getChangeRequestId());
        staging.setDataType(mapToStagingDataType(batch.getDataType()));
        staging.setOperationType(BulkImportStaging.OperationType.UPSERT);
        staging.setSourceSystem(batch.getSourceSystem());
        staging.setSourceFileName(batch.getSourceFileName());
        staging.setSourceFileChecksum(batch.getSourceFileChecksum());
        staging.setRowNumber(rowNumber);
        staging.setTargetTable(targetTable);
        staging.setCreatedBy(userId);
        staging.setUpdatedBy(userId);

        // Set natural key based on data type
        staging.setNaturalKey(naturalKey);

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

    @Transactional
    public List<UUID> importRecords(List<Map<String, String>> records, String entityType) {
        List<UUID> successfullyProcessedChangeRequestIds = new ArrayList<>();
        for (Map<String, String> record : records) {
            logger.info("Importing record: {}", record);
            ChangeRequestDto changeRequest = createChangeRequestForSingleImport("single-import-user", entityType, record);
            logger.info("Created change request: {}", changeRequest.getId());
            try {
                switch (entityType) {
                    case "COUNTRIES":
                        processSingleCountryRecord(record, changeRequest.getId());
                        break;
                    default:
                        throw new UnsupportedOperationException("Processing not implemented for data type: " + entityType);
                }
                successfullyProcessedChangeRequestIds.add(changeRequest.getId());
            } catch (Exception e) {
                logger.error("Failed to import record: {}", record, e);
                throw new RuntimeException("Failed to import record: " + e.getMessage(), e);
            }
        }
        return successfullyProcessedChangeRequestIds;
    }

    private ChangeRequestDto createChangeRequestForSingleImport(String userId, String entityType, Map<String, String> record) {
        ChangeRequestDto changeRequest = new ChangeRequestDto();
        changeRequest.setTitle("Single Import: " + entityType);
        changeRequest.setOperationType("IMPORT");
        changeRequest.setEntityType(entityType);
        changeRequest.setRequestedBy(userId);
        changeRequest.setPriority(2); // MEDIUM priority
        changeRequest.setBusinessJustification("Single data import from UI");
        try {
            changeRequest.setProposedChanges(objectMapper.writeValueAsString(record));
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize record: {}", record, e);
        }
        return changeRequestService.create(changeRequest);
    }
    
    private void processSingleCountryRecord(Map<String, String> dataMap, UUID changeRequestId) throws Exception {
        logger.info("Processing single country record: {}, changeRequestId: {}", dataMap, changeRequestId);
        // Create new Country entity
        Country country = new Country();
        country.setCountryCode(dataMap.get("countryCode"));
        country.setCountryName(dataMap.get("countryName"));
        country.setIso2Code(dataMap.get("iso2Code"));
        country.setIso3Code(dataMap.get("iso3Code"));
        country.setNumericCode(dataMap.get("numericCode"));
        country.setIsActive(Boolean.parseBoolean(dataMap.getOrDefault("isActive", "true")));
        country.setRecordedBy("single-import-user");
        country.setChangeRequestId(changeRequestId);

        // Retrieve and set CodeSystem
        String codeSystemName = dataMap.get("codeSystem");
        if (codeSystemName == null || codeSystemName.isBlank()) {
            throw new IllegalArgumentException("codeSystem is required for Country records.");
        }
        gov.dhs.cbp.reference.core.entity.CodeSystem codeSystem = codeSystemRepository.findByCode(codeSystemName)
            .orElseThrow(() -> new IllegalArgumentException("CodeSystem not found: " + codeSystemName));
        country.setCodeSystem(codeSystem);

        // Save the country
        try {
            countryRepository.save(country);
        } catch (Exception e) {
            logger.error("Failed to save country: {}", country, e);
            throw e;
        }
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
        private final List<gov.dhs.cbp.reference.api.dto.ValidationErrorDto> errors = new ArrayList<>();
        private final List<gov.dhs.cbp.reference.api.dto.ValidationErrorDto> warnings = new ArrayList<>();
        private final List<Map<String, String>> previewData = new ArrayList<>();
        private static final int MAX_PREVIEW_RECORDS = 15;

        public void incrementValid() { validCount++; }
        public void incrementInvalid() { invalidCount++; }
        public void incrementWarning() { warningCount++; }

        public int getValidCount() { return validCount; }
        public int getInvalidCount() { return invalidCount; }
        public int getWarningCount() { return warningCount; }

        public List<gov.dhs.cbp.reference.api.dto.ValidationErrorDto> getErrors() { return errors; }
        public List<gov.dhs.cbp.reference.api.dto.ValidationErrorDto> getWarnings() { return warnings; }
        public List<Map<String, String>> getPreviewData() { return previewData; }

        public void addError(gov.dhs.cbp.reference.api.dto.ValidationErrorDto error) {
            this.errors.add(error);
        }

        public void addWarning(gov.dhs.cbp.reference.api.dto.ValidationErrorDto warning) {
            this.warnings.add(warning);
        }
        
        public void addPreviewData(Map<String, String> data) {
            if (this.previewData.size() < MAX_PREVIEW_RECORDS) {
                this.previewData.add(data);
            }
        }
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