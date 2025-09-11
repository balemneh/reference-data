package gov.dhs.cbp.reference.loaders.iso.service;

import com.opencsv.bean.CsvToBeanBuilder;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import gov.dhs.cbp.reference.events.publisher.EventPublisherService;
import gov.dhs.cbp.reference.events.model.ReferenceDataEvent;
import gov.dhs.cbp.reference.loader.common.*;
import gov.dhs.cbp.reference.loaders.iso.entity.IsoCountryStaging;
import gov.dhs.cbp.reference.loaders.iso.model.IsoCountryData;
import gov.dhs.cbp.reference.loaders.iso.repository.IsoCountryStagingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Validator;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IsoCountryLoader extends AbstractLoader<IsoCountryData, Country, IsoCountryStaging> {
    
    private static final Logger logger = LoggerFactory.getLogger(IsoCountryLoader.class);
    private static final String ISO_CODE_SYSTEM = "ISO3166-1";
    
    @Autowired
    private IsoCountryStagingRepository stagingRepository;
    
    @Autowired
    private CountryRepository countryRepository;
    
    @Autowired
    private CodeSystemRepository codeSystemRepository;
    
    @Autowired
    private EventPublisherService eventPublisherService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${loader.iso.source-url:https://raw.githubusercontent.com/datasets/country-codes/master/data/country-codes.csv}")
    private String sourceUrl;
    
    @Value("${loader.iso.local-file:classpath:data/iso-countries.csv}")
    private String localFile;
    
    @Value("${loader.iso.use-local:false}")
    private boolean useLocalFile;
    
    private CodeSystem isoCodeSystem;
    
    public IsoCountryLoader(
            JobLauncher jobLauncher,
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            LoaderConfiguration configuration,
            Validator validator) {
        super(
            jobLauncher,
            jobRepository,
            transactionManager,
            configuration,
            new ValidationService<>(validator),
            null // Will initialize diff detector separately
        );
        
        // Initialize custom validation rules
        this.validationService.addRule(new DuplicateCodeValidation());
        this.validationService.addRule(new RegionValidation());
    }
    
    @Override
    protected String getLoaderName() {
        return "ISO-3166 Country Loader";
    }
    
    @Override
    protected List<IsoCountryData> extractData(LoaderContext context) throws Exception {
        logger.info("Extracting ISO country data from: {}", useLocalFile ? localFile : sourceUrl);
        
        List<IsoCountryData> data;
        
        if (useLocalFile) {
            data = loadFromLocalFile();
        } else {
            data = loadFromUrl();
        }
        
        logger.info("Extracted {} ISO country records", data.size());
        return data;
    }
    
    private List<IsoCountryData> loadFromLocalFile() throws Exception {
        Path path = Paths.get(localFile.replace("classpath:", "src/main/resources/"));
        
        try (Reader reader = Files.newBufferedReader(path)) {
            return new CsvToBeanBuilder<IsoCountryData>(reader)
                .withType(IsoCountryData.class)
                .withIgnoreLeadingWhiteSpace(true)
                .build()
                .parse();
        }
    }
    
    private List<IsoCountryData> loadFromUrl() throws Exception {
        // Download file to temp location
        Path tempFile = Files.createTempFile("iso-countries-", ".csv");
        
        try {
            URL url = new URL(sourceUrl);
            try (InputStream in = url.openStream()) {
                Files.copy(in, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Parse CSV
            try (Reader reader = Files.newBufferedReader(tempFile)) {
                return new CsvToBeanBuilder<IsoCountryData>(reader)
                    .withType(IsoCountryData.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
    
    @Override
    protected List<IsoCountryStaging> transformToStaging(
            List<IsoCountryData> sourceData, 
            ValidationResult validationResult) {
        
        return sourceData.stream()
            .map(this::mapToStaging)
            .collect(Collectors.toList());
    }
    
    private IsoCountryStaging mapToStaging(IsoCountryData source) {
        IsoCountryStaging staging = new IsoCountryStaging();
        
        staging.setCountryName(source.getName());
        staging.setAlpha2Code(source.getAlpha2Code());
        staging.setAlpha3Code(source.getAlpha3Code());
        staging.setNumericCode(source.getNumericCode());
        // Fields not available in basic ISO data model - set defaults
        staging.setOfficialName(source.getName());
        staging.setCommonName(source.getName());
        staging.setCapital(null);
        staging.setRegion(source.getRegion());
        staging.setSubregion(source.getSubRegion());
        staging.setContinent(null);
        staging.setIsIndependent(null);
        staging.setIsUnMember(null);
        staging.setCurrencyCode(null);
        staging.setCurrencyName(null);
        staging.setPhoneCode(null);
        staging.setTld(null);
        staging.setLanguages(null);
        staging.setPopulation(null);
        staging.setAreaSqKm(null);
        staging.setLatitude(null);
        staging.setLongitude(null);
        
        // Calculate source hash for change detection
        staging.setSourceHash(calculateHash(source));
        staging.setSourceFile(useLocalFile ? localFile : sourceUrl);
        staging.setSourceDate(LocalDateTime.now().toString());
        
        return staging;
    }
    
    private String calculateHash(IsoCountryData data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String content = String.format("%s|%s|%s|%s|%s|%s",
                data.getAlpha2Code(),
                data.getAlpha3Code(),
                data.getName(),
                data.getNumericCode(),
                data.getRegion(),
                data.getSubRegion()
            );
            byte[] hash = md.digest(content.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            logger.error("Failed to calculate hash", e);
            return null;
        }
    }
    
    @Override
    protected Country transformToEntity(IsoCountryStaging staging) {
        Country country = new Country();
        
        country.setCountryCode(staging.getAlpha3Code());
        country.setCountryName(staging.getCountryName());
        country.setIso2Code(staging.getAlpha2Code());
        country.setIso3Code(staging.getAlpha3Code());
        country.setNumericCode(staging.getNumericCode());
        country.setCodeSystem(getOrCreateCodeSystem());
        country.setValidFrom(LocalDate.now());
        country.setRecordedAt(LocalDateTime.now());
        country.setRecordedBy("ISO_LOADER");
        country.setIsActive(true);
        
        // Store additional data as metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("officialName", staging.getOfficialName());
        metadata.put("commonName", staging.getCommonName());
        metadata.put("capital", staging.getCapital());
        metadata.put("region", staging.getRegion());
        metadata.put("subregion", staging.getSubregion());
        metadata.put("continent", staging.getContinent());
        metadata.put("isIndependent", staging.getIsIndependent());
        metadata.put("isUnMember", staging.getIsUnMember());
        metadata.put("currencyCode", staging.getCurrencyCode());
        metadata.put("currencyName", staging.getCurrencyName());
        metadata.put("phoneCode", staging.getPhoneCode());
        metadata.put("tld", staging.getTld());
        metadata.put("languages", staging.getLanguages());
        metadata.put("population", staging.getPopulation());
        metadata.put("areaSqKm", staging.getAreaSqKm());
        metadata.put("latitude", staging.getLatitude());
        metadata.put("longitude", staging.getLongitude());
        
        country.setMetadata(convertToJson(metadata));
        
        return country;
    }
    
    private String convertToJson(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(map);
        } catch (Exception e) {
            logger.error("Failed to convert metadata to JSON", e);
            return "{}";
        }
    }
    
    @Override
    protected Country updateEntity(Country current, IsoCountryStaging staged) {
        Country updated = new Country();
        
        // Copy current entity
        updated.setId(null); // New version gets new ID
        updated.setCountryCode(current.getCountryCode());
        updated.setCodeSystem(current.getCodeSystem());
        updated.setVersion(current.getVersion() + 1);
        
        // Update with new data
        updated.setCountryName(staged.getCountryName());
        updated.setIso2Code(staged.getAlpha2Code());
        updated.setIso3Code(staged.getAlpha3Code());
        updated.setNumericCode(staged.getNumericCode());
        updated.setIsActive(true);
        
        // Set bitemporal fields
        updated.setValidFrom(LocalDate.now());
        updated.setRecordedAt(LocalDateTime.now());
        updated.setRecordedBy("ISO_LOADER");
        
        // Update metadata
        updated.setMetadata(transformToEntity(staged).getMetadata());
        
        return updated;
    }
    
    @Override
    @Transactional
    protected void saveEntity(Country entity, LoaderContext context) {
        entity.setChangeRequestId(context.getChangeRequestId());
        Country saved = countryRepository.save(entity);
        
        // Publish event
        ReferenceDataEvent.EventType eventType = saved.getVersion() == 1 ? 
            ReferenceDataEvent.EventType.CREATED : 
            ReferenceDataEvent.EventType.UPDATED;
        
        eventPublisherService.publishCountryEvent(saved, eventType, "ISO_LOADER");
    }
    
    @Override
    @Transactional
    protected void markAsDeleted(Country entity, LoaderContext context) {
        // In bitemporal model, we don't delete - we end validity
        entity.setValidTo(LocalDate.now());
        entity.setIsActive(false);
        Country saved = countryRepository.save(entity);
        
        // Publish deletion event
        eventPublisherService.publishCountryEvent(saved, ReferenceDataEvent.EventType.DELETED, "ISO_LOADER");
    }
    
    @Override
    protected List<Country> getCurrentProductionData() {
        return countryRepository.findCurrentBySystemCode(ISO_CODE_SYSTEM, 
            org.springframework.data.domain.Pageable.unpaged()).getContent();
    }
    
    @Override
    @Transactional
    protected void saveStagingBatch(List<IsoCountryStaging> batch) {
        stagingRepository.saveAll(batch);
    }
    
    @Override
    @Transactional
    protected void clearStagingTables() {
        stagingRepository.deleteAll();
    }
    
    @Override
    protected void publishEvents(DiffResult<IsoCountryStaging, Country> diffResult, String executionId) {
        // Events are now published individually in saveEntity and markAsDeleted methods
        // This method can be used for batch-level events or summary statistics
        
        logger.info("ISO country load completed with {} changes for execution: {}", 
                   diffResult.getTotalChanges(), executionId);
    }
    
    @Override
    protected String createChangeRequest(DiffResult<IsoCountryStaging, Country> diffResult, LoaderContext context) {
        // This would integrate with the workflow module to create a change request
        // For now, return a mock ID
        String changeRequestId = "CR-ISO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        logger.info("Created change request: {} with {} changes", changeRequestId, diffResult.getTotalChanges());
        return changeRequestId;
    }
    
    @Override
    protected LocalDateTime getLastSuccessfulRunTime() {
        // Query from loader execution history table
        // For now, return 30 days ago as default
        return LocalDateTime.now().minusDays(30);
    }
    
    @Override
    protected void saveLoaderResult(LoaderResult result) {
        // Save to loader execution history table
        logger.info("Loader execution completed: {}", result);
    }
    
    @Override
    protected void handleLoadFailure(Exception e, LoaderContext context) {
        logger.error("ISO loader failed for execution: {}", context.getExecutionId(), e);
        // Send alert, create incident, etc.
    }
    
    @Scheduled(cron = "${loader.iso.schedule:0 0 2 * * SUN}")
    public void scheduledLoad() {
        if (!configuration.isEnableScheduling()) {
            return;
        }
        
        logger.info("Starting scheduled ISO country load");
        LoaderContext context = new LoaderContext(
            UUID.randomUUID().toString(),
            "SCHEDULER"
        );
        executeLoad(context);
    }
    
    private CodeSystem getOrCreateCodeSystem() {
        if (isoCodeSystem == null) {
            isoCodeSystem = codeSystemRepository.findByCode(ISO_CODE_SYSTEM)
                .orElseGet(() -> {
                    CodeSystem cs = new CodeSystem();
                    cs.setCode(ISO_CODE_SYSTEM);
                    cs.setName("ISO 3166-1 Country Codes");
                    cs.setDescription("International Organization for Standardization country codes");
                    cs.setOwner("ISO");
                    cs.setIsActive(true);
                    return codeSystemRepository.save(cs);
                });
        }
        return isoCodeSystem;
    }
    
    // Custom validation rules
    private static class DuplicateCodeValidation implements ValidationService.ValidationRule<IsoCountryData> {
        private final Set<String> seenAlpha2 = new HashSet<>();
        private final Set<String> seenAlpha3 = new HashSet<>();
        
        @Override
        public String getName() {
            return "DuplicateCodeCheck";
        }
        
        @Override
        public Result validate(IsoCountryData record) {
            if (seenAlpha2.contains(record.getAlpha2Code())) {
                return Result.invalid("Duplicate Alpha-2 code: " + record.getAlpha2Code());
            }
            if (seenAlpha3.contains(record.getAlpha3Code())) {
                return Result.invalid("Duplicate Alpha-3 code: " + record.getAlpha3Code());
            }
            
            seenAlpha2.add(record.getAlpha2Code());
            seenAlpha3.add(record.getAlpha3Code());
            
            return Result.valid();
        }
    }
    
    private static class RegionValidation implements ValidationService.ValidationRule<IsoCountryData> {
        private static final Set<String> VALID_REGIONS = Set.of(
            "Africa", "Americas", "Asia", "Europe", "Oceania", "Antarctica"
        );
        
        @Override
        public String getName() {
            return "RegionValidation";
        }
        
        @Override
        public Result validate(IsoCountryData record) {
            if (record.getRegion() != null && !VALID_REGIONS.contains(record.getRegion())) {
                return Result.warning("Unknown region: " + record.getRegion());
            }
            return Result.valid();
        }
    }
}