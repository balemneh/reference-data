package gov.dhs.cbp.reference.loaders.iso.loader;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import gov.dhs.cbp.reference.events.publisher.EventPublisherService;
import gov.dhs.cbp.reference.events.model.ReferenceDataEvent;
import gov.dhs.cbp.reference.loaders.iso.model.IsoCountryData;
import gov.dhs.cbp.reference.loaders.iso.service.DiffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.*;

@Configuration
@EnableBatchProcessing
public class IsoCountryLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(IsoCountryLoader.class);
    
    @Autowired
    private CountryRepository countryRepository;
    
    @Autowired
    private CodeSystemRepository codeSystemRepository;
    
    @Autowired
    private EventPublisherService eventPublisherService;
    
    @Autowired
    private DiffService diffService;
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Bean
    public Job isoCountryLoaderJob() {
        return new JobBuilder("isoCountryLoaderJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .flow(loadIsoCountriesStep())
                .end()
                .build();
    }
    
    @Bean
    public Step loadIsoCountriesStep() {
        return new StepBuilder("loadIsoCountriesStep", jobRepository)
                .<IsoCountryData, Country>chunk(100, transactionManager)
                .reader(isoCountryReader())
                .processor(isoCountryProcessor())
                .writer(isoCountryWriter())
                .build();
    }
    
    @Bean
    public ItemReader<IsoCountryData> isoCountryReader() {
        try {
            List<IsoCountryData> countries = loadIsoCountriesFromCsv();
            return new ListItemReader<>(countries);
        } catch (Exception e) {
            logger.error("Failed to load ISO country data", e);
            return new ListItemReader<>(Collections.emptyList());
        }
    }
    
    @Bean
    public ItemProcessor<IsoCountryData, Country> isoCountryProcessor() {
        return new ItemProcessor<IsoCountryData, Country>() {
            @Override
            @Transactional
            public Country process(IsoCountryData item) {
                CodeSystem isoSystem = codeSystemRepository.findByCode("ISO3166-1")
                        .orElseThrow(() -> new RuntimeException("ISO3166-1 code system not found"));
                
                Optional<Country> existing = countryRepository.findCurrentByCodeAndSystemCode(
                        item.getAlpha3Code(), "ISO3166-1");
                
                if (existing.isPresent()) {
                    Country existingCountry = existing.get();
                    if (hasChanged(existingCountry, item)) {
                        Country newVersion = createNewVersion(existingCountry, item, isoSystem);
                        return newVersion;
                    }
                    return null;
                } else {
                    Country country = new Country();
                    country.setCodeSystem(isoSystem);
                    country.setCountryCode(item.getAlpha3Code());
                    country.setCountryName(item.getName());
                    country.setIso2Code(item.getAlpha2Code());
                    country.setIso3Code(item.getAlpha3Code());
                    country.setNumericCode(item.getNumericCode());
                    country.setValidFrom(LocalDate.now());
                    country.setRecordedBy("ISO_LOADER");
                    country.setChangeRequestId("ISO_IMPORT_" + LocalDate.now());
                    return country;
                }
            }
        };
    }
    
    @Bean
    public ItemWriter<Country> isoCountryWriter() {
        return new ItemWriter<Country>() {
            @Override
            @Transactional
            public void write(org.springframework.batch.item.Chunk<? extends Country> chunk) {
                for (Country country : chunk.getItems()) {
                    if (country != null) {
                        Country saved = countryRepository.save(country);
                        publishCountryEvent(saved);
                    }
                }
            }
        };
    }
    
    private List<IsoCountryData> loadIsoCountriesFromCsv() throws IOException, CsvException {
        ClassPathResource resource = new ClassPathResource("data/iso-countries.csv");
        List<IsoCountryData> countries = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
            List<String[]> records = reader.readAll();
            
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                if (record.length >= 4) {
                    IsoCountryData data = new IsoCountryData();
                    data.setName(record[0]);
                    data.setAlpha2Code(record[1]);
                    data.setAlpha3Code(record[2]);
                    data.setNumericCode(record[3]);
                    countries.add(data);
                }
            }
        }
        
        return countries;
    }
    
    private boolean hasChanged(Country existing, IsoCountryData newData) {
        return !existing.getCountryName().equals(newData.getName()) ||
               !Objects.equals(existing.getIso2Code(), newData.getAlpha2Code()) ||
               !Objects.equals(existing.getNumericCode(), newData.getNumericCode());
    }
    
    private Country createNewVersion(Country existing, IsoCountryData newData, CodeSystem codeSystem) {
        existing.setValidTo(LocalDate.now());
        countryRepository.save(existing);
        
        Country newVersion = new Country();
        newVersion.setCodeSystem(codeSystem);
        newVersion.setCountryCode(newData.getAlpha3Code());
        newVersion.setCountryName(newData.getName());
        newVersion.setIso2Code(newData.getAlpha2Code());
        newVersion.setIso3Code(newData.getAlpha3Code());
        newVersion.setNumericCode(newData.getNumericCode());
        newVersion.setValidFrom(LocalDate.now());
        newVersion.setRecordedBy("ISO_LOADER");
        newVersion.setChangeRequestId("ISO_UPDATE_" + LocalDate.now());
        newVersion.setVersion(existing.getVersion() + 1);
        
        return newVersion;
    }
    
    private void publishCountryEvent(Country country) {
        try {
            ReferenceDataEvent.EventType eventType = country.getVersion() == 1 ? 
                ReferenceDataEvent.EventType.CREATED : 
                ReferenceDataEvent.EventType.UPDATED;
            
            eventPublisherService.publishCountryEvent(country, eventType, "ISO_LOADER");
            
            logger.debug("Published {} event for country {}", eventType, country.getCountryCode());
        } catch (Exception e) {
            logger.error("Failed to publish event for country {}", country.getId(), e);
        }
    }
}