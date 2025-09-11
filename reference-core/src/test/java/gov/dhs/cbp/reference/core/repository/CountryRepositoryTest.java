package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.config.TestSchemaConfig;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.entity.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(TestSchemaConfig.class)
@org.junit.jupiter.api.Disabled("H2 does not support PostgreSQL schemas properly - requires TestContainers with real PostgreSQL")
class CountryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CountryRepository countryRepository;

    private CodeSystem codeSystem;
    private Country country;

    @BeforeEach
    void setUp() {
        // Create and persist a code system
        codeSystem = new CodeSystem();
        codeSystem.setCode("ISO3166-1");
        codeSystem.setName("ISO Country Codes");
        codeSystem.setDescription("ISO standard for country codes");
        codeSystem.setOwner("ISO");
        codeSystem.setIsActive(true);
        codeSystem = entityManager.persist(codeSystem);
        
        // Create and persist a country
        country = new Country();
        country.setCountryCode("US");
        country.setCountryName("United States");
        country.setIso2Code("US");
        country.setIso3Code("USA");
        country.setNumericCode("840");
        country.setCodeSystem(codeSystem);
        country.setIsActive(true);
        country.setValidFrom(LocalDate.now().minusYears(1));
        country.setValidTo(null);
        country.setRecordedAt(LocalDateTime.now());
        country.setRecordedBy("test-user");
        country = entityManager.persist(country);
        
        entityManager.flush();
    }

    @Test
    void testFindById() {
        Optional<Country> found = countryRepository.findById(country.getId());
        
        assertTrue(found.isPresent());
        assertEquals("United States", found.get().getCountryName());
        assertEquals("USA", found.get().getIso3Code());
    }

    @Test
    void testFindCurrentById() {
        Optional<Country> found = countryRepository.findCurrentById(country.getId());
        
        assertTrue(found.isPresent());
        assertEquals("United States", found.get().getCountryName());
    }

    @Test
    void testFindCurrentByCodeAndSystemCode() {
        Optional<Country> found = countryRepository.findCurrentByCodeAndSystemCode("US", "ISO3166-1");
        
        assertTrue(found.isPresent());
        assertEquals("United States", found.get().getCountryName());
        assertEquals("US", found.get().getCountryCode());
    }

    @Test
    void testFindByCodeAndSystemAsOf() {
        LocalDate asOfDate = LocalDate.now();
        Optional<Country> found = countryRepository.findByCodeAndSystemAsOf("US", "ISO3166-1", asOfDate);
        
        assertTrue(found.isPresent());
        assertEquals("United States", found.get().getCountryName());
    }

    @Test
    void testFindCurrentBySystemCode() {
        Page<Country> page = countryRepository.findCurrentBySystemCode("ISO3166-1", PageRequest.of(0, 10));
        
        assertFalse(page.isEmpty());
        assertEquals(1, page.getTotalElements());
        assertEquals("United States", page.getContent().get(0).getCountryName());
    }

    @Test
    void testFindAllCurrent() {
        List<Country> countries = countryRepository.findAllCurrent();
        
        assertFalse(countries.isEmpty());
        assertEquals(1, countries.size());
        assertEquals("United States", countries.get(0).getCountryName());
    }

    @Test
    void testSearchByName() {
        Page<Country> page = countryRepository.searchByName("United", PageRequest.of(0, 10));
        
        assertFalse(page.isEmpty());
        assertEquals(1, page.getTotalElements());
        assertEquals("United States", page.getContent().get(0).getCountryName());
    }

    @Test
    void testSearchByNameCaseInsensitive() {
        Page<Country> page = countryRepository.searchByName("united", PageRequest.of(0, 10));
        
        assertFalse(page.isEmpty());
        assertEquals(1, page.getTotalElements());
        assertEquals("United States", page.getContent().get(0).getCountryName());
    }

    @Test
    void testFindByValidFromRange() {
        LocalDate startDate = LocalDate.now().minusYears(2);
        LocalDate endDate = LocalDate.now();
        
        // Find all countries and filter by valid from date range
        List<Country> allCountries = countryRepository.findAll();
        List<Country> countries = allCountries.stream()
                .filter(c -> c.getValidFrom() != null)
                .filter(c -> !c.getValidFrom().isBefore(startDate) && !c.getValidFrom().isAfter(endDate))
                .toList();
        
        assertFalse(countries.isEmpty());
        assertEquals(1, countries.size());
        assertEquals("United States", countries.get(0).getCountryName());
    }

    @Test
    void testSaveNewCountry() {
        Country newCountry = new Country();
        newCountry.setCountryCode("CA");
        newCountry.setCountryName("Canada");
        newCountry.setIso2Code("CA");
        newCountry.setIso3Code("CAN");
        newCountry.setNumericCode("124");
        newCountry.setCodeSystem(codeSystem);
        newCountry.setIsActive(true);
        newCountry.setValidFrom(LocalDate.now());
        newCountry.setRecordedAt(LocalDateTime.now());
        newCountry.setRecordedBy("test-user");
        
        Country saved = countryRepository.save(newCountry);
        
        assertNotNull(saved.getId());
        assertEquals("Canada", saved.getCountryName());
        assertEquals("CAN", saved.getIso3Code());
    }

    @Test
    void testUpdateCountry() {
        country.setCountryName("United States of America");
        Country updated = countryRepository.save(country);
        
        assertEquals("United States of America", updated.getCountryName());
        assertEquals(country.getId(), updated.getId());
    }

    @Test
    void testFindInactiveCountries() {
        // Create an inactive country
        Country inactiveCountry = new Country();
        inactiveCountry.setCountryCode("XX");
        inactiveCountry.setCountryName("Inactive Country");
        inactiveCountry.setIso2Code("XX");
        inactiveCountry.setIso3Code("XXX");
        inactiveCountry.setNumericCode("999");
        inactiveCountry.setCodeSystem(codeSystem);
        inactiveCountry.setIsActive(false);
        inactiveCountry.setValidFrom(LocalDate.now());
        inactiveCountry.setRecordedAt(LocalDateTime.now());
        inactiveCountry.setRecordedBy("test-user");
        entityManager.persist(inactiveCountry);
        entityManager.flush();
        
        List<Country> allCountries = countryRepository.findAll();
        long inactiveCount = allCountries.stream()
                .filter(c -> !c.getIsActive())
                .count();
        
        assertEquals(1, inactiveCount);
    }
}