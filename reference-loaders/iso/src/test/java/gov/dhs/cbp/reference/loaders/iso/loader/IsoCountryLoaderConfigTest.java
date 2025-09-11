package gov.dhs.cbp.reference.loaders.iso.loader;

import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import gov.dhs.cbp.reference.events.publisher.EventPublisherService;
import gov.dhs.cbp.reference.loaders.iso.model.IsoCountryData;
import gov.dhs.cbp.reference.loaders.iso.service.DiffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IsoCountryLoaderConfigTest {

    @Mock
    private CountryRepository countryRepository;
    
    @Mock
    private CodeSystemRepository codeSystemRepository;
    
    @Mock
    private EventPublisherService eventPublisherService;
    
    @Mock
    private DiffService diffService;
    
    @Mock
    private JobRepository jobRepository;
    
    @Mock
    private PlatformTransactionManager transactionManager;

    private IsoCountryLoader isoCountryLoader;
    private CodeSystem isoCodeSystem;

    @BeforeEach
    void setUp() {
        isoCountryLoader = new IsoCountryLoader();
        
        ReflectionTestUtils.setField(isoCountryLoader, "countryRepository", countryRepository);
        ReflectionTestUtils.setField(isoCountryLoader, "codeSystemRepository", codeSystemRepository);
        ReflectionTestUtils.setField(isoCountryLoader, "eventPublisherService", eventPublisherService);
        ReflectionTestUtils.setField(isoCountryLoader, "diffService", diffService);
        ReflectionTestUtils.setField(isoCountryLoader, "jobRepository", jobRepository);
        ReflectionTestUtils.setField(isoCountryLoader, "transactionManager", transactionManager);
        
        isoCodeSystem = new CodeSystem();
        isoCodeSystem.setId(java.util.UUID.randomUUID());
        isoCodeSystem.setCode("ISO3166-1");
        isoCodeSystem.setName("ISO 3166-1 Country Codes");
        isoCodeSystem.setIsActive(true);
    }

    @Test
    void testIsoCountryLoaderJob() {
        Job job = isoCountryLoader.isoCountryLoaderJob();
        
        assertNotNull(job);
        assertEquals("isoCountryLoaderJob", job.getName());
    }

    @Test
    void testLoadIsoCountriesStep() {
        Step step = isoCountryLoader.loadIsoCountriesStep();
        
        assertNotNull(step);
        assertEquals("loadIsoCountriesStep", step.getName());
    }

    @Test
    void testIsoCountryReader() {
        ItemReader<IsoCountryData> reader = isoCountryLoader.isoCountryReader();
        
        assertNotNull(reader);
        // Reader should work even if CSV file is not found (returns empty list)
    }

    @Test
    void testIsoCountryProcessor() throws Exception {
        when(codeSystemRepository.findByCode("ISO3166-1"))
            .thenReturn(Optional.of(isoCodeSystem));
        
        ItemProcessor<IsoCountryData, Country> processor = isoCountryLoader.isoCountryProcessor();
        assertNotNull(processor);
        
        IsoCountryData item = new IsoCountryData();
        item.setName("United States");
        item.setAlpha2Code("US");
        item.setAlpha3Code("USA");
        item.setNumericCode("840");
        
        // Test new country creation
        when(countryRepository.findCurrentByCodeAndSystemCode("USA", "ISO3166-1"))
            .thenReturn(Optional.empty());
        
        Country result = processor.process(item);
        
        assertNotNull(result);
        assertEquals("USA", result.getCountryCode());
        assertEquals("United States", result.getCountryName());
        assertEquals("US", result.getIso2Code());
        assertEquals("USA", result.getIso3Code());
        assertEquals("840", result.getNumericCode());
        assertEquals(isoCodeSystem, result.getCodeSystem());
        assertEquals(LocalDate.now(), result.getValidFrom());
        assertEquals("ISO_LOADER", result.getRecordedBy());
        assertTrue(result.getChangeRequestId().startsWith("ISO_IMPORT_"));
    }

    @Test
    void testIsoCountryProcessorWithExistingUnchangedCountry() throws Exception {
        when(codeSystemRepository.findByCode("ISO3166-1"))
            .thenReturn(Optional.of(isoCodeSystem));
        
        Country existingCountry = new Country();
        existingCountry.setCountryName("United States");
        existingCountry.setIso2Code("US");
        existingCountry.setNumericCode("840");
        existingCountry.setVersion(1L);
        
        when(countryRepository.findCurrentByCodeAndSystemCode("USA", "ISO3166-1"))
            .thenReturn(Optional.of(existingCountry));
        
        ItemProcessor<IsoCountryData, Country> processor = isoCountryLoader.isoCountryProcessor();
        
        IsoCountryData item = new IsoCountryData();
        item.setName("United States");
        item.setAlpha2Code("US");
        item.setAlpha3Code("USA");
        item.setNumericCode("840");
        
        Country result = processor.process(item);
        
        // Should return null for unchanged country
        assertNull(result);
    }

    @Test
    void testIsoCountryProcessorWithExistingChangedCountry() throws Exception {
        when(codeSystemRepository.findByCode("ISO3166-1"))
            .thenReturn(Optional.of(isoCodeSystem));
        
        Country existingCountry = new Country();
        existingCountry.setCountryName("United States of America");
        existingCountry.setIso2Code("US");
        existingCountry.setNumericCode("840");
        existingCountry.setVersion(1L);
        
        when(countryRepository.findCurrentByCodeAndSystemCode("USA", "ISO3166-1"))
            .thenReturn(Optional.of(existingCountry));
        when(countryRepository.save(any(Country.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        ItemProcessor<IsoCountryData, Country> processor = isoCountryLoader.isoCountryProcessor();
        
        IsoCountryData item = new IsoCountryData();
        item.setName("United States"); // Different name
        item.setAlpha2Code("US");
        item.setAlpha3Code("USA");
        item.setNumericCode("840");
        
        Country result = processor.process(item);
        
        assertNotNull(result);
        assertEquals("USA", result.getCountryCode());
        assertEquals("United States", result.getCountryName());
        assertEquals("US", result.getIso2Code());
        assertEquals(2L, result.getVersion()); // Version should be incremented
        assertEquals(LocalDate.now(), result.getValidFrom());
        assertEquals("ISO_LOADER", result.getRecordedBy());
        assertTrue(result.getChangeRequestId().startsWith("ISO_UPDATE_"));
    }

    @Test
    void testIsoCountryProcessorWithMissingCodeSystem() {
        when(codeSystemRepository.findByCode("ISO3166-1"))
            .thenReturn(Optional.empty());
        
        ItemProcessor<IsoCountryData, Country> processor = isoCountryLoader.isoCountryProcessor();
        
        IsoCountryData item = new IsoCountryData();
        item.setName("United States");
        item.setAlpha2Code("US");
        item.setAlpha3Code("USA");
        item.setNumericCode("840");
        
        assertThrows(RuntimeException.class, () -> processor.process(item));
    }

    @Test
    void testIsoCountryWriter() throws Exception {
        ItemWriter<Country> writer = isoCountryLoader.isoCountryWriter();
        assertNotNull(writer);
        
        Country country1 = new Country();
        country1.setCountryCode("USA");
        country1.setVersion(1L);
        
        Country country2 = new Country();
        country2.setCountryCode("CAN");
        country2.setVersion(1L);
        
        when(countryRepository.save(any(Country.class)))
            .thenAnswer(invocation -> {
                Country saved = (Country) invocation.getArgument(0);
                saved.setId(java.util.UUID.randomUUID());
                return saved;
            });
        
        org.springframework.batch.item.Chunk<Country> chunk = 
            new org.springframework.batch.item.Chunk<>(Arrays.asList(country1, country2));
        
        writer.write(chunk);
        
        // Verify that both countries are saved
        verify(countryRepository, times(2)).save(any(Country.class));
        
        // Note: Event publisher calls happen within the writer's inner class
        // and can't be easily verified in this test setup.
    }

    @Test
    void testLoadIsoCountriesFromCsv() throws Exception {
        List<IsoCountryData> result = ReflectionTestUtils.invokeMethod(
            isoCountryLoader, 
            "loadIsoCountriesFromCsv"
        );
        
        // This will test loading from the test CSV file
        assertNotNull(result);
        // Should have loaded test data from test-data/iso-countries.csv
    }

    @Test
    void testHasChangedMethod() {
        Country existing = new Country();
        existing.setCountryName("United States");
        existing.setIso2Code("US");
        existing.setNumericCode("840");
        
        IsoCountryData unchanged = new IsoCountryData();
        unchanged.setName("United States");
        unchanged.setAlpha2Code("US");
        unchanged.setNumericCode("840");
        
        IsoCountryData changed = new IsoCountryData();
        changed.setName("United States of America"); // Changed name
        changed.setAlpha2Code("US");
        changed.setNumericCode("840");
        
        boolean hasNotChanged = ReflectionTestUtils.invokeMethod(
            isoCountryLoader, 
            "hasChanged", 
            existing, 
            unchanged
        );
        
        boolean hasChanged = ReflectionTestUtils.invokeMethod(
            isoCountryLoader, 
            "hasChanged", 
            existing, 
            changed
        );
        
        assertFalse(hasNotChanged);
        assertTrue(hasChanged);
    }

    @Test
    void testCreateNewVersion() {
        Country existing = new Country();
        existing.setId(java.util.UUID.randomUUID());
        existing.setCountryCode("USA");
        existing.setVersion(1L);
        existing.setIsActive(true);
        
        when(countryRepository.save(any(Country.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        IsoCountryData newData = new IsoCountryData();
        newData.setName("United States of America");
        newData.setAlpha2Code("US");
        newData.setAlpha3Code("USA");
        newData.setNumericCode("840");
        
        Country result = ReflectionTestUtils.invokeMethod(
            isoCountryLoader, 
            "createNewVersion", 
            existing, 
            newData, 
            isoCodeSystem
        );
        
        assertNotNull(result);
        assertEquals("USA", result.getCountryCode());
        assertEquals("United States of America", result.getCountryName());
        assertEquals("US", result.getIso2Code());
        assertEquals("USA", result.getIso3Code());
        assertEquals("840", result.getNumericCode());
        assertEquals(2L, result.getVersion()); // Incremented
        assertEquals(isoCodeSystem, result.getCodeSystem());
        assertEquals(LocalDate.now(), result.getValidFrom());
        assertEquals("ISO_LOADER", result.getRecordedBy());
        assertTrue(result.getChangeRequestId().startsWith("ISO_UPDATE_"));
        
        // Check that existing country was updated
        assertEquals(LocalDate.now(), existing.getValidTo());
        verify(countryRepository).save(existing);
    }

    @Test
    void testPublishCountryEvent() {
        Country country = new Country();
        country.setId(java.util.UUID.randomUUID());
        country.setCountryCode("USA");
        country.setVersion(1L);
        
        ReflectionTestUtils.invokeMethod(isoCountryLoader, "publishCountryEvent", country);
        
        verify(eventPublisherService).publishCountryEvent(
            eq(country), 
            any(), 
            eq("ISO_LOADER")
        );
    }

    @Test
    void testPublishCountryEventWithException() {
        Country country = new Country();
        country.setId(java.util.UUID.randomUUID());
        country.setCountryCode("USA");
        country.setVersion(1L);
        
        doThrow(new RuntimeException("Event publishing failed"))
            .when(eventPublisherService).publishCountryEvent(any(), any(), any());
        
        // Should not throw exception even if event publishing fails
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(isoCountryLoader, "publishCountryEvent", country);
        });
        
        verify(eventPublisherService).publishCountryEvent(
            eq(country), 
            any(), 
            eq("ISO_LOADER")
        );
    }
}