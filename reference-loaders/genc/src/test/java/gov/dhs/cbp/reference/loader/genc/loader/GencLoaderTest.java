package gov.dhs.cbp.reference.loader.genc.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import gov.dhs.cbp.reference.events.publisher.EventPublisherService;
import gov.dhs.cbp.reference.loader.common.*;
import gov.dhs.cbp.reference.loader.genc.entity.GencEntityStaging;
import gov.dhs.cbp.reference.loader.genc.loader.GencLoader;
import gov.dhs.cbp.reference.loader.genc.model.GencData;
import gov.dhs.cbp.reference.loader.genc.repository.GencEntityStagingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class GencLoaderTest {
    
    @Mock
    private JobLauncher jobLauncher;
    
    @Mock
    private JobRepository jobRepository;
    
    @Mock
    private PlatformTransactionManager transactionManager;
    
    @Mock
    private LoaderConfiguration configuration;
    
    @Mock
    private ValidationService<GencData> validationService;
    
    @Mock
    private DiffDetector<GencEntityStaging, Country> diffDetector;
    
    @Mock
    private GencEntityStagingRepository stagingRepository;
    
    @Mock
    private CountryRepository countryRepository;
    
    @Mock
    private CodeSystemRepository codeSystemRepository;
    
    @Mock
    private EventPublisherService eventPublisherService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private GencLoader gencLoader;
    private CodeSystem gencCodeSystem;
    
    @BeforeEach
    void setUp() {
        gencLoader = new GencLoader(
            jobLauncher,
            jobRepository,
            transactionManager,
            configuration,
            validationService,
            diffDetector,
            stagingRepository,
            countryRepository,
            codeSystemRepository,
            eventPublisherService,
            objectMapper
        );
        
        gencCodeSystem = new CodeSystem();
        gencCodeSystem.setCode("GENC");
        gencCodeSystem.setName("Geopolitical Entities, Names, and Codes");
        
        when(codeSystemRepository.findByCode("GENC"))
            .thenReturn(Optional.of(gencCodeSystem));
            
        when(configuration.getBatchSize()).thenReturn(1000);
        when(configuration.isPublishEvents()).thenReturn(true);
    }
    
    @Test
    void testTransformToStaging() {
        // Given
        GencData sourceData = createGencData("USA", "United States");
        List<GencData> dataList = Arrays.asList(sourceData);
        ValidationResult validationResult = new ValidationResult();
        
        // When
        List<GencEntityStaging> stagingList = gencLoader.transformToStaging(dataList, validationResult);
        
        // Then
        assertEquals(1, stagingList.size());
        GencEntityStaging staging = stagingList.get(0);
        assertEquals("United States", staging.getEntityName());
        assertEquals("United States", staging.getName());
        assertEquals("US", staging.getChar2Code());
        assertEquals("USA", staging.getChar3Code());
        assertEquals("840", staging.getNumericCode());
        assertEquals("current", staging.getGencStatus());
        assertEquals("country", staging.getEntityType());
        assertEquals("Washington D.C.", staging.getCapital());
        assertEquals("Americas", staging.getRegion());
    }
    
    @Test
    void testTransformToEntity() {
        // Given
        GencEntityStaging staging = createGencEntityStaging("CAN", "Canada");
        
        // When
        Country country = gencLoader.transformToEntity(staging);
        
        // Then
        assertEquals("Canada", country.getCountryName());
        assertEquals("CA", country.getIso2Code());
        assertEquals("CAN", country.getIso3Code());
        assertEquals("124", country.getNumericCode());
        assertEquals("CAN", country.getCountryCode());
        assertTrue(country.getIsActive());
        assertNotNull(country.getValidFrom());
        assertEquals("GENC_LOADER", country.getRecordedBy());
    }
    
    @Test
    void testGetCurrentProductionData() {
        // Given
        List<Country> countries = Arrays.asList(new Country(), new Country());
        when(countryRepository.findAll()).thenReturn(countries);
        
        // When
        List<Country> result = gencLoader.getCurrentProductionData();
        
        // Then
        assertEquals(2, result.size());
        verify(countryRepository).findAll();
    }
    
    @Test
    void testSaveStagingBatch() {
        // Given
        List<GencEntityStaging> batch = Arrays.asList(
            createGencEntityStaging("USA", "United States"),
            createGencEntityStaging("CAN", "Canada")
        );
        
        // When
        gencLoader.saveStagingBatch(batch);
        
        // Then
        verify(stagingRepository).saveAll(batch);
    }
    
    @Test
    void testClearStagingTables() {
        // When
        gencLoader.clearStagingTables();
        
        // Then
        verify(stagingRepository).deleteAll();
    }
    
    @Test
    void testUpdateEntity() {
        // Given
        Country existing = new Country();
        existing.setCountryName("Old Name");
        existing.setIso2Code("XX");
        existing.setNumericCode("000");
        existing.setIsActive(false);
        
        GencEntityStaging staged = createGencEntityStaging("USA", "United States");
        
        // When
        Country updated = gencLoader.updateEntity(existing, staged);
        
        // Then
        assertEquals("United States", updated.getCountryName());
        assertEquals("US", updated.getIso2Code());
        assertEquals("840", updated.getNumericCode());
        assertTrue(updated.getIsActive());
    }
    
    @Test
    void testSaveEntity() {
        // Given
        Country country = new Country();
        country.setCountryName("Test Country");
        LoaderContext context = new LoaderContext();
        
        Country saved = new Country();
        // saved.setId(UUID.randomUUID()); // ID is set by JPA
        saved.setCountryName("Test Country");
        
        when(countryRepository.save(any(Country.class))).thenReturn(saved);
        
        // When
        gencLoader.saveEntity(country, context);
        
        // Then
        verify(countryRepository).save(country);
        verify(eventPublisherService).publishCountryEvent(any(), any(), eq("GENC_LOADER"));
    }
    
    @Test
    void testMarkAsDeleted() {
        // Given
        Country country = new Country();
        // country.setId(UUID.randomUUID()); // ID is set by JPA
        country.setCountryName("Test Country");
        country.setIsActive(true);
        LoaderContext context = new LoaderContext();
        
        Country saved = new Country();
        // saved.setId(UUID.randomUUID()); // ID is set by JPA
        saved.setIsActive(false);
        
        when(countryRepository.save(any(Country.class))).thenReturn(saved);
        
        // When
        gencLoader.markAsDeleted(country, context);
        
        // Then
        assertFalse(country.getIsActive());
        assertNotNull(country.getValidTo());
        verify(countryRepository).save(country);
        verify(eventPublisherService).publishCountryEvent(any(), any(), eq("GENC_LOADER"));
    }
    
    @Test
    void testExtractDataWithNoFile() throws Exception {
        // Given
        LoaderContext context = new LoaderContext();
        
        // When
        List<GencData> result = gencLoader.extractData(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    private GencData createGencData(String code3, String name) {
        GencData data = new GencData();
        data.setGenc3Code(code3);
        data.setGenc2Code(code3.substring(0, 2));
        data.setGencNumeric(code3.equals("USA") ? "840" : "124");
        data.setName(name);
        data.setFullName(name);
        data.setStatus("current");
        data.setEntityType("country");
        data.setCapital(code3.equals("USA") ? "Washington D.C." : "Ottawa");
        data.setRegion("Americas");
        data.setSubRegion("North America");
        return data;
    }
    
    private GencEntityStaging createGencEntityStaging(String code3, String name) {
        GencEntityStaging staging = new GencEntityStaging();
        staging.setGenc3Code(code3);
        staging.setChar3Code(code3);
        staging.setGenc2Code(code3.equals("USA") ? "US" : "CA");
        staging.setChar2Code(code3.equals("USA") ? "US" : "CA");
        staging.setGencNumeric(code3.equals("USA") ? "840" : "124");
        staging.setNumericCode(code3.equals("USA") ? "840" : "124");
        staging.setName(name);
        staging.setEntityName(name);
        staging.setFullName(name);
        staging.setGencStatus("current");
        staging.setStatus("current");
        staging.setEntityType("country");
        staging.setCapital(code3.equals("USA") ? "Washington D.C." : "Ottawa");
        staging.setRegion("Americas");
        staging.setSubregion("North America");
        staging.setPoliticalStatus("independent");
        return staging;
    }
}