package gov.dhs.cbp.reference.loader.genc;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GencLoaderService {
    
    private static final Logger log = LoggerFactory.getLogger(GencLoaderService.class);
    
    private final CountryRepository countryRepository;
    private final CodeSystemRepository codeSystemRepository;
    private final ObjectMapper objectMapper;
    
    public GencLoaderService(CountryRepository countryRepository,
                            CodeSystemRepository codeSystemRepository) {
        this.countryRepository = countryRepository;
        this.codeSystemRepository = codeSystemRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    @Transactional
    public LoadResult loadGencData(String filePath) {
        log.info("Starting GENC data load from: {}", filePath);
        
        LoadResult result = new LoadResult();
        
        try {
            // Get or create GENC code system
            CodeSystem gencSystem = codeSystemRepository.findByCode("GENC")
                    .orElseGet(() -> {
                        CodeSystem system = new CodeSystem();
                        system.setCode("GENC");
                        system.setName("Geopolitical Entities, Names, and Codes");
                        system.setDescription("NGA standard for geopolitical entities");
                        system.setOwner("NGA");
                        system.setIsActive(true);
                        return codeSystemRepository.save(system);
                    });
            
            List<GencRecord> records = parseGencFile(filePath);
            log.info("Parsed {} GENC records", records.size());
            
            for (GencRecord record : records) {
                try {
                    processGencRecord(record, gencSystem, result);
                } catch (Exception e) {
                    log.error("Error processing GENC record: {}", record, e);
                    result.addError("Failed to process record: " + record.getGenc3());
                }
            }
            
            log.info("GENC data load completed. Created: {}, Updated: {}, Errors: {}",
                    result.getCreatedCount(), result.getUpdatedCount(), result.getErrorCount());
            
        } catch (Exception e) {
            log.error("Error loading GENC data", e);
            result.addError("Fatal error: " + e.getMessage());
        }
        
        return result;
    }
    
    private List<GencRecord> parseGencFile(String filePath) throws IOException, CsvException {
        List<GencRecord> records = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> lines = reader.readAll();
            
            // Skip header
            for (int i = 1; i < lines.size(); i++) {
                String[] line = lines.get(i);
                if (line.length >= 5) {
                    GencRecord record = new GencRecord();
                    record.setGenc2(line[0]);
                    record.setGenc3(line[1]);
                    record.setGencNumeric(line[2]);
                    record.setGencName(line[3]);
                    record.setGencShortName(line[4]);
                    
                    if (line.length > 5) record.setCapital(line[5]);
                    if (line.length > 6) record.setRegion(line[6]);
                    if (line.length > 7) record.setSubregion(line[7]);
                    if (line.length > 8) record.setStatus(line[8]);
                    
                    records.add(record);
                }
            }
        }
        
        return records;
    }
    
    private void processGencRecord(GencRecord record, CodeSystem codeSystem, LoadResult result) {
        // Check if country already exists
        Optional<Country> existing = countryRepository.findByCountryCodeAndSystem(
                record.getGenc3(), "GENC");
        
        if (existing.isPresent()) {
            Country country = existing.get();
            
            // Check if update is needed
            if (!country.getCountryName().equals(record.getGencName()) ||
                !Objects.equals(country.getAlpha2Code(), record.getGenc2())) {
                
                // Create new version
                country.setValidTo(LocalDate.now());
                countryRepository.save(country);
                
                Country newVersion = new Country();
                newVersion.setCodeSystem(codeSystem);
                newVersion.setCountryCode(record.getGenc3());
                newVersion.setCountryName(record.getGencName());
                newVersion.setAlpha2Code(record.getGenc2());
                newVersion.setAlpha3Code(record.getGenc3());
                newVersion.setNumericCode(record.getGencNumeric());
                newVersion.setIsActive(!"Deprecated".equals(record.getStatus()));
                newVersion.setVersion(country.getVersion() + 1);
                newVersion.setValidFrom(LocalDate.now());
                newVersion.setRecordedBy("GENC_LOADER");
                
                // Add metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("shortName", record.getGencShortName());
                metadata.put("capital", record.getCapital());
                metadata.put("region", record.getRegion());
                metadata.put("subregion", record.getSubregion());
                metadata.put("status", record.getStatus());
                try {
                    newVersion.setMetadata(objectMapper.writeValueAsString(metadata));
                } catch (Exception e) {
                    log.warn("Failed to serialize metadata", e);
                }
                
                countryRepository.save(newVersion);
                result.incrementUpdated();
                
                log.debug("Updated GENC country: {}", record.getGenc3());
            }
        } else {
            // Create new country
            Country country = new Country();
            country.setCodeSystem(codeSystem);
            country.setCountryCode(record.getGenc3());
            country.setCountryName(record.getGencName());
            country.setAlpha2Code(record.getGenc2());
            country.setAlpha3Code(record.getGenc3());
            country.setNumericCode(record.getGencNumeric());
            country.setIsActive(!"Deprecated".equals(record.getStatus()));
            country.setVersion(1L);
            country.setValidFrom(LocalDate.now());
            country.setRecordedBy("GENC_LOADER");
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("shortName", record.getGencShortName());
            metadata.put("capital", record.getCapital());
            metadata.put("region", record.getRegion());
            metadata.put("subregion", record.getSubregion());
            metadata.put("status", record.getStatus());
            try {
                country.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) {
                log.warn("Failed to serialize metadata", e);
            }
            
            countryRepository.save(country);
            result.incrementCreated();
            
            log.debug("Created GENC country: {}", record.getGenc3());
        }
    }
    
    // Data classes
    public static class GencRecord {
        private String genc2;
        private String genc3;
        private String gencNumeric;
        private String gencName;
        private String gencShortName;
        private String capital;
        private String region;
        private String subregion;
        private String status;
        
        // Getters and setters
        public String getGenc2() { return genc2; }
        public void setGenc2(String genc2) { this.genc2 = genc2; }
        
        public String getGenc3() { return genc3; }
        public void setGenc3(String genc3) { this.genc3 = genc3; }
        
        public String getGencNumeric() { return gencNumeric; }
        public void setGencNumeric(String gencNumeric) { this.gencNumeric = gencNumeric; }
        
        public String getGencName() { return gencName; }
        public void setGencName(String gencName) { this.gencName = gencName; }
        
        public String getGencShortName() { return gencShortName; }
        public void setGencShortName(String gencShortName) { this.gencShortName = gencShortName; }
        
        public String getCapital() { return capital; }
        public void setCapital(String capital) { this.capital = capital; }
        
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        
        public String getSubregion() { return subregion; }
        public void setSubregion(String subregion) { this.subregion = subregion; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    public static class LoadResult {
        private int createdCount = 0;
        private int updatedCount = 0;
        private List<String> errors = new ArrayList<>();
        
        public void incrementCreated() { createdCount++; }
        public void incrementUpdated() { updatedCount++; }
        public void addError(String error) { errors.add(error); }
        
        public int getCreatedCount() { return createdCount; }
        public int getUpdatedCount() { return updatedCount; }
        public int getErrorCount() { return errors.size(); }
        public List<String> getErrors() { return errors; }
    }
}