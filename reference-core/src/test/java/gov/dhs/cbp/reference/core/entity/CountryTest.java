package gov.dhs.cbp.reference.core.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CountryTest {
    
    private Country country;
    private CodeSystem codeSystem;
    
    @BeforeEach
    void setUp() {
        country = new Country();
        codeSystem = new CodeSystem();
        codeSystem.setCode("ISO3166-1");
        codeSystem.setName("ISO Country Codes");
    }
    
    @Test
    void testCountryCreation() {
        country.setCountryName("United States");
        country.setCountryCode("US");
        country.setIso2Code("US");
        country.setIso3Code("USA");
        country.setNumericCode("840");
        country.setIsActive(true);
        country.setCodeSystem(codeSystem);
        
        assertEquals("United States", country.getCountryName());
        assertEquals("US", country.getCountryCode());
        assertEquals("US", country.getIso2Code());
        assertEquals("USA", country.getIso3Code());
        assertEquals("840", country.getNumericCode());
        assertTrue(country.getIsActive());
        assertEquals(codeSystem, country.getCodeSystem());
    }
    
    @Test
    void testAliasMethods() {
        country.setAlpha2Code("US");
        country.setAlpha3Code("USA");
        
        assertEquals("US", country.getAlpha2Code());
        assertEquals("USA", country.getAlpha3Code());
        assertEquals("US", country.getIso2Code());
        assertEquals("USA", country.getIso3Code());
    }
    
    @Test
    void testBitemporalFields() {
        UUID id = UUID.randomUUID();
        LocalDate validFrom = LocalDate.now();
        LocalDate validTo = LocalDate.now().plusYears(1);
        LocalDateTime recordedAt = LocalDateTime.now();
        
        country.setId(id);
        country.setValidFrom(validFrom);
        country.setValidTo(validTo);
        country.setRecordedAt(recordedAt);
        country.setRecordedBy("test-user");
        country.setVersion(1L);
        country.setIsCorrection(false);
        
        assertEquals(id, country.getId());
        assertEquals(validFrom, country.getValidFrom());
        assertEquals(validTo, country.getValidTo());
        assertEquals(recordedAt, country.getRecordedAt());
        assertEquals("test-user", country.getRecordedBy());
        assertEquals(1L, country.getVersion());
        assertFalse(country.getIsCorrection());
    }
    
    @Test
    void testMetadata() {
        String metadata = "{\"population\": 330000000, \"capital\": \"Washington D.C.\"}";
        country.setMetadata(metadata);
        
        assertEquals(metadata, country.getMetadata());
    }
    
    @Test
    void testIsCurrentlyValid() {
        LocalDate now = LocalDate.now();
        country.setValidFrom(now.minusDays(10));
        country.setValidTo(now.plusDays(10));
        
        assertTrue(country.isCurrentlyValid());
    }
    
    @Test
    void testWasValidOn() {
        LocalDate now = LocalDate.now();
        country.setValidFrom(now.minusDays(10));
        country.setValidTo(now.plusDays(10));
        
        assertTrue(country.wasValidOn(now));
        assertFalse(country.wasValidOn(now.minusDays(20)));
        assertFalse(country.wasValidOn(now.plusDays(20)));
    }
    
    @Test
    void testWasValidOnWithNullValidTo() {
        LocalDate now = LocalDate.now();
        country.setValidFrom(now.minusDays(10));
        country.setValidTo(null);
        
        assertTrue(country.wasValidOn(now));
        assertTrue(country.wasValidOn(now.plusDays(100)));
        assertFalse(country.wasValidOn(now.minusDays(20)));
    }
    
    @Test
    void testChangeRequestId() {
        String changeRequestId = "CR-2025-001";
        country.setChangeRequestId(changeRequestId);
        
        assertEquals(changeRequestId, country.getChangeRequestId());
    }
}