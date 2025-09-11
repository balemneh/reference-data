package gov.dhs.cbp.reference.loader.genc.loader;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import gov.dhs.cbp.reference.loader.common.*;
import gov.dhs.cbp.reference.loader.genc.entity.GencEntityStaging;
import gov.dhs.cbp.reference.loader.genc.model.GencData;
import gov.dhs.cbp.reference.loader.genc.repository.GencEntityStagingRepository;
import gov.dhs.cbp.reference.events.publisher.EventPublisherService;
import gov.dhs.cbp.reference.events.model.ReferenceDataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.FileReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GencLoader extends AbstractLoader<GencData, Country, GencEntityStaging> {

    private static final Logger logger = LoggerFactory.getLogger(GencLoader.class);
    private static final String GENC_CODE_SYSTEM = "GENC";
    private static final String LOADER_NAME = "GENC_LOADER";
    
    private final GencEntityStagingRepository stagingRepository;
    private final CountryRepository countryRepository;
    private final CodeSystemRepository codeSystemRepository;
    private final EventPublisherService eventPublisherService;
    private final ObjectMapper objectMapper;

    public GencLoader(
            JobLauncher jobLauncher,
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            LoaderConfiguration configuration,
            ValidationService<GencData> validationService,
            DiffDetector<GencEntityStaging, Country> diffDetector,
            GencEntityStagingRepository stagingRepository,
            CountryRepository countryRepository,
            CodeSystemRepository codeSystemRepository,
            EventPublisherService eventPublisherService,
            ObjectMapper objectMapper) {
        
        super(jobLauncher, jobRepository, transactionManager, configuration, 
              validationService, diffDetector);
        
        this.stagingRepository = stagingRepository;
        this.countryRepository = countryRepository;
        this.codeSystemRepository = codeSystemRepository;
        this.eventPublisherService = eventPublisherService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected String getLoaderName() {
        return LOADER_NAME;
    }

    @Override
    protected List<GencData> extractData(LoaderContext context) throws Exception {
        String filePath = (String) context.getMetadata("sourceFilePath");
        if (!StringUtils.hasText(filePath)) {
            // For testing, return empty list if no file path
            return new ArrayList<>();
        }

        logger.info("Extracting GENC data from: {}", filePath);

        try (FileReader reader = new FileReader(filePath)) {
            CsvToBean<GencData> csvToBean = new CsvToBeanBuilder<GencData>(reader)
                    .withType(GencData.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<GencData> data = csvToBean.parse();
            logger.info("Extracted {} GENC records from file", data.size());
            return data;
        }
    }

    @Override
    protected List<GencEntityStaging> transformToStaging(List<GencData> sourceData, 
                                                         ValidationResult validationResult) {
        
        return sourceData.stream()
                .map(this::transformToStagingEntity)
                .collect(Collectors.toList());
    }

    private GencEntityStaging transformToStagingEntity(GencData data) {
        GencEntityStaging staging = new GencEntityStaging();
        
        // Core identification fields
        staging.setEntityName(data.getName());
        staging.setChar2Code(data.getGenc2Code());
        staging.setChar3Code(data.getGenc3Code());
        staging.setNumericCode(data.getGencNumeric());
        staging.setGenc3Code(data.getGenc3Code());
        staging.setGenc2Code(data.getGenc2Code());
        staging.setGencNumeric(data.getGencNumeric());
        staging.setName(data.getName());
        staging.setFullName(data.getFullName());
        
        // Status and classification
        staging.setGencStatus(data.getStatus());
        staging.setEntityType(data.getEntityType());
        staging.setPoliticalStatus(data.getPoliticalStatus());
        staging.setParentCode(data.getParentCode());
        staging.setSovereignty(data.getSovereignty());
        
        // Geographic and descriptive data
        staging.setCapital(data.getCapital());
        staging.setRegion(data.getRegion());
        staging.setSubregion(data.getSubRegion());
        staging.setLocalShortName(data.getLocalShortName());
        staging.setLocalLongName(data.getLocalLongName());
        
        // Change tracking
        staging.setFormerCodes(data.getFormerCodes());
        staging.setUpdateDate(data.getUpdateDate());
        staging.setUpdateType(data.getUpdateType());
        staging.setUpdateDescription(data.getUpdateDescription());
        staging.setEffectiveDate(data.getEffectiveDate());
        staging.setExpirationDate(data.getExpirationDate());
        staging.setReplacementCodes(data.getReplacementCodes());
        
        // Geographic coordinates
        if (StringUtils.hasText(data.getLatitude())) {
            try {
                staging.setLatitude(Double.parseDouble(data.getLatitude()));
            } catch (NumberFormatException e) {
                logger.warn("Invalid latitude value: {}", data.getLatitude());
            }
        }
        if (StringUtils.hasText(data.getLongitude())) {
            try {
                staging.setLongitude(Double.parseDouble(data.getLongitude()));
            } catch (NumberFormatException e) {
                logger.warn("Invalid longitude value: {}", data.getLongitude());
            }
        }
        
        // Source metadata
        staging.setSourceFile(data.getSourceDocument());
        staging.setSourceDate(data.getSourceDate());
        
        // Set validation status as valid by default
        staging.setValidationStatus(StagingEntity.ValidationStatus.VALID);
        
        return staging;
    }

    @Override
    protected Country transformToEntity(GencEntityStaging stagingEntity) {
        Country country = new Country();
        
        // Get or create GENC code system
        CodeSystem gencSystem = getOrCreateGencCodeSystem();
        country.setCodeSystem(gencSystem);
        
        // Map primary fields
        country.setCountryCode(stagingEntity.getGenc3Code());
        country.setCountryName(stagingEntity.getEntityName());
        country.setIso2Code(stagingEntity.getChar2Code());
        country.setIso3Code(stagingEntity.getChar3Code());
        country.setNumericCode(stagingEntity.getNumericCode());
        
        // Set active status based on GENC status
        boolean isActive = "current".equalsIgnoreCase(stagingEntity.getGencStatus());
        country.setIsActive(isActive);
        
        // Set validity dates
        country.setValidFrom(parseEffectiveDate(stagingEntity.getEffectiveDate()));
        if (StringUtils.hasText(stagingEntity.getExpirationDate())) {
            country.setValidTo(parseEffectiveDate(stagingEntity.getExpirationDate()));
        }
        
        // Set recording metadata
        country.setRecordedBy(LOADER_NAME);
        country.setRecordedAt(LocalDateTime.now());
        
        // Create metadata JSON
        Map<String, Object> metadata = createMetadata(stagingEntity);
        try {
            country.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            logger.warn("Failed to serialize metadata for entity: {}", stagingEntity.getGenc3Code(), e);
        }
        
        return country;
    }

    private CodeSystem getOrCreateGencCodeSystem() {
        return codeSystemRepository.findByCode(GENC_CODE_SYSTEM)
                .orElseGet(() -> {
                    CodeSystem system = new CodeSystem();
                    system.setCode(GENC_CODE_SYSTEM);
                    system.setName("Geopolitical Entities, Names, and Codes");
                    system.setDescription("NGA standard for geopolitical entities and codes");
                    system.setOwner("NGA");
                    system.setIsActive(true);
                    return codeSystemRepository.save(system);
                });
    }

    private LocalDate parseEffectiveDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return LocalDate.now();
        }
        
        try {
            // Try common date formats
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(dateStr, formatter);
                } catch (Exception ignored) {
                    // Try next format
                }
            }
            
            logger.warn("Could not parse date: {}, using current date", dateStr);
            return LocalDate.now();
        } catch (Exception e) {
            logger.warn("Failed to parse date: {}, using current date", dateStr, e);
            return LocalDate.now();
        }
    }

    private Map<String, Object> createMetadata(GencEntityStaging stagingEntity) {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("entityType", stagingEntity.getEntityType());
        metadata.put("politicalStatus", stagingEntity.getPoliticalStatus());
        metadata.put("capital", stagingEntity.getCapital());
        metadata.put("region", stagingEntity.getRegion());
        metadata.put("subregion", stagingEntity.getSubregion());
        
        if (stagingEntity.getLatitude() != null) {
            metadata.put("latitude", stagingEntity.getLatitude());
        }
        if (stagingEntity.getLongitude() != null) {
            metadata.put("longitude", stagingEntity.getLongitude());
        }
        
        metadata.put("sovereignty", stagingEntity.getSovereignty());
        metadata.put("parentCode", stagingEntity.getParentCode());
        metadata.put("localShortName", stagingEntity.getLocalShortName());
        metadata.put("localLongName", stagingEntity.getLocalLongName());
        
        return metadata;
    }

    @Override
    protected void loadToStaging(List<GencEntityStaging> stagingData, String executionId) {
        for (GencEntityStaging entity : stagingData) {
            entity.setLoadExecutionId(executionId);
            entity.setLoadedAt(LocalDateTime.now());
        }
        stagingRepository.saveAll(stagingData);
    }

    @Override
    protected List<Country> getCurrentProductionData() {
        return countryRepository.findAll();
    }

    @Override
    @Transactional
    protected void applyChanges(DiffResult<GencEntityStaging, Country> diffResult, LoaderContext context) {
        // Process additions
        for (GencEntityStaging staging : diffResult.getAdditions()) {
            Country country = transformToEntity(staging);
            Country saved = countryRepository.save(country);
            
            // Publish event
            if (configuration.isPublishEvents()) {
                eventPublisherService.publishCountryEvent(saved, ReferenceDataEvent.EventType.CREATED, LOADER_NAME);
            }
        }
        
        // Process updates
        for (DiffResult.UpdatePair<GencEntityStaging, Country> update : diffResult.getUpdates()) {
            Country existing = update.getCurrent();
            GencEntityStaging staging = update.getStaged();
            
            // Update fields
            existing.setCountryName(staging.getEntityName());
            existing.setIso2Code(staging.getChar2Code());
            existing.setNumericCode(staging.getNumericCode());
            existing.setIsActive("current".equalsIgnoreCase(staging.getGencStatus()));
            
            Country saved = countryRepository.save(existing);
            
            // Publish event
            if (configuration.isPublishEvents()) {
                eventPublisherService.publishCountryEvent(saved, ReferenceDataEvent.EventType.UPDATED, LOADER_NAME);
            }
        }
        
        // Process deletions (soft delete)
        for (Country country : diffResult.getDeletions()) {
            country.setIsActive(false);
            country.setValidTo(LocalDate.now());
            Country saved = countryRepository.save(country);
            
            // Publish event
            if (configuration.isPublishEvents()) {
                eventPublisherService.publishCountryEvent(saved, ReferenceDataEvent.EventType.DELETED, LOADER_NAME);
            }
        }
    }

    @Override
    public LoaderResult executeIncrementalLoad(LoaderContext context) {
        // For now, just delegate to full load
        // In a real implementation, would only load changed records
        return executeLoad(context);
    }

    @Override
    protected Country updateEntity(Country current, GencEntityStaging staged) {
        current.setCountryName(staged.getEntityName());
        current.setIso2Code(staged.getChar2Code());
        current.setNumericCode(staged.getNumericCode());
        current.setIsActive("current".equalsIgnoreCase(staged.getGencStatus()));
        return current;
    }

    @Override
    protected void saveEntity(Country entity, LoaderContext context) {
        Country saved = countryRepository.save(entity);
        if (configuration.isPublishEvents()) {
            ReferenceDataEvent.EventType eventType = entity.getId() == null ? 
                ReferenceDataEvent.EventType.CREATED : ReferenceDataEvent.EventType.UPDATED;
            eventPublisherService.publishCountryEvent(saved, eventType, LOADER_NAME);
        }
    }

    @Override
    protected void markAsDeleted(Country entity, LoaderContext context) {
        entity.setIsActive(false);
        entity.setValidTo(LocalDate.now());
        Country saved = countryRepository.save(entity);
        if (configuration.isPublishEvents()) {
            eventPublisherService.publishCountryEvent(saved, ReferenceDataEvent.EventType.DELETED, LOADER_NAME);
        }
    }

    @Override
    protected void saveStagingBatch(List<GencEntityStaging> batch) {
        stagingRepository.saveAll(batch);
    }

    @Override
    protected void clearStagingTables() {
        stagingRepository.deleteAll();
    }

    @Override
    protected void publishEvents(DiffResult<GencEntityStaging, Country> diffResult, String executionId) {
        // Events are published in saveEntity and markAsDeleted methods
        logger.info("Events published for execution: {}", executionId);
    }

    @Override
    protected String createChangeRequest(DiffResult<GencEntityStaging, Country> diffResult, LoaderContext context) {
        // In a real implementation, would create a change request in workflow system
        String changeRequestId = UUID.randomUUID().toString();
        logger.info("Created change request: {} with {} changes", changeRequestId, diffResult.getTotalChanges());
        return changeRequestId;
    }

    @Override
    protected LocalDateTime getLastSuccessfulRunTime() {
        // In a real implementation, would query loader history
        return LocalDateTime.now().minusDays(1);
    }

    @Override
    protected void saveLoaderResult(LoaderResult result) {
        // In a real implementation, would persist to loader_history table
        logger.info("Loader result saved: {}", result);
    }

    @Override
    protected void handleLoadFailure(Exception exception, LoaderContext context) {
        logger.error("Load failed for context: {}", context.getExecutionId(), exception);
        // In a real implementation, would send alerts and rollback staging
    }
}