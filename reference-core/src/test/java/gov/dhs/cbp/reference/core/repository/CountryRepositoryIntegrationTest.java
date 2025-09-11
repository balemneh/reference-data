package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.config.H2TestConfiguration;
import gov.dhs.cbp.reference.core.config.TestEntityConfiguration;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.entity.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({H2TestConfiguration.class, TestEntityConfiguration.class})
@ActiveProfiles("integration-test")
@Sql(scripts = "classpath:schema-h2-no-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CountryRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private CodeSystemRepository codeSystemRepository;

    private CodeSystem isoCodeSystem;
    private CodeSystem cbpCodeSystem;

    @BeforeEach
    void setUp() {
        // Create code systems
        isoCodeSystem = new CodeSystem();
        isoCodeSystem.setCode("ISO3166-1");
        isoCodeSystem.setName("ISO 3166-1 Country Codes");
        isoCodeSystem.setDescription("International standard for country codes");
        isoCodeSystem.setOwner("ISO");
        // Let Hibernate manage ID and timestamps
        isoCodeSystem = codeSystemRepository.saveAndFlush(isoCodeSystem);

        cbpCodeSystem = new CodeSystem();
        cbpCodeSystem.setCode("CBP-COUNTRY5");
        cbpCodeSystem.setName("CBP 5-Character Country Codes");
        cbpCodeSystem.setDescription("CBP internal country code system");
        cbpCodeSystem.setOwner("CBP");
        // Let Hibernate manage ID and timestamps
        cbpCodeSystem = codeSystemRepository.saveAndFlush(cbpCodeSystem);
    }

    @Test
    @DisplayName("Should save and retrieve country by ID")
    void testSaveAndFindById() {
        // Given
        Country country = createCountry("US", "United States", isoCodeSystem);
        
        // When
        Country savedCountry = countryRepository.saveAndFlush(country);
        Optional<Country> foundCountry = countryRepository.findById(savedCountry.getId());
        
        // Then
        assertThat(foundCountry).isPresent();
        assertThat(foundCountry.get().getCountryCode()).isEqualTo("US");
        assertThat(foundCountry.get().getCountryName()).isEqualTo("United States");
        assertThat(foundCountry.get().getCodeSystem().getCode()).isEqualTo("ISO3166-1");
    }

    @Test
    @DisplayName("Should find country by code and code system")
    void testFindByCountryCodeAndCodeSystem() {
        // Given
        Country usCountry = createCountry("US", "United States", isoCodeSystem);
        Country caCountry = createCountry("CA", "Canada", isoCodeSystem);
        countryRepository.saveAndFlush(usCountry);
        countryRepository.saveAndFlush(caCountry);
        
        // When
        Optional<Country> foundCountry = countryRepository.findByCountryCodeAndCodeSystem("US", isoCodeSystem);
        
        // Then
        assertThat(foundCountry).isPresent();
        assertThat(foundCountry.get().getCountryName()).isEqualTo("United States");
    }

    @Test
    @DisplayName("Should find all active countries")
    void testFindAllActive() {
        // Given
        Country activeCountry = createCountry("US", "United States", isoCodeSystem);
        activeCountry.setIsActive(true);
        
        Country inactiveCountry = createCountry("YU", "Yugoslavia", isoCodeSystem);
        inactiveCountry.setIsActive(false);
        inactiveCountry.setValidTo(LocalDate.now().minusDays(1));
        
        countryRepository.saveAndFlush(activeCountry);
        countryRepository.saveAndFlush(inactiveCountry);
        
        // When
        List<Country> activeCountries = countryRepository.findAllActive();
        
        // Then
        assertThat(activeCountries).hasSize(1);
        assertThat(activeCountries.get(0).getCountryCode()).isEqualTo("US");
        assertThat(activeCountries.get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should find countries by code system with pagination")
    void testFindByCodeSystemWithPagination() {
        // Given
        createAndSaveCountries();
        PageRequest pageRequest = PageRequest.of(0, 2);
        
        // When
        Page<Country> countriesPage = countryRepository.findByCodeSystem(isoCodeSystem, pageRequest);
        
        // Then
        assertThat(countriesPage.getContent()).hasSize(2);
        assertThat(countriesPage.getTotalElements()).isEqualTo(3);
        assertThat(countriesPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find countries valid at specific date")
    void testFindValidAtDate() {
        // Given
        LocalDate queryDate = LocalDate.now();
        
        Country currentCountry = createCountry("US", "United States", isoCodeSystem);
        currentCountry.setValidFrom(queryDate.minusDays(10));
        currentCountry.setValidTo(queryDate.plusDays(10));
        
        Country expiredCountry = createCountry("YU", "Yugoslavia", isoCodeSystem);
        expiredCountry.setValidFrom(queryDate.minusDays(100));
        expiredCountry.setValidTo(queryDate.minusDays(10));
        
        Country futureCountry = createCountry("XX", "Future Country", isoCodeSystem);
        futureCountry.setValidFrom(queryDate.plusDays(10));
        futureCountry.setValidTo(null);
        
        countryRepository.saveAndFlush(currentCountry);
        countryRepository.saveAndFlush(expiredCountry);
        countryRepository.saveAndFlush(futureCountry);
        
        // When
        List<Country> validCountries = countryRepository.findValidAtDate(queryDate);
        
        // Then
        assertThat(validCountries).hasSize(1);
        assertThat(validCountries.get(0).getCountryCode()).isEqualTo("US");
    }

    @Test
    @DisplayName("Should handle bitemporal versioning correctly")
    void testBitemporalVersioning() {
        // Given
        Country originalCountry = createCountry("US", "United States", isoCodeSystem);
        originalCountry.setVersion(1L);
        originalCountry.setValidFrom(LocalDate.now().minusDays(10));
        Country savedOriginal = countryRepository.saveAndFlush(originalCountry);
        
        // When - Create a new version (correction) as a separate entity
        Country updatedCountry = createCountry("US", "United States of America", isoCodeSystem);
        updatedCountry.setVersion(2L);
        updatedCountry.setValidFrom(LocalDate.now().minusDays(10)); // Same valid_from
        updatedCountry.setIsCorrection(true);
        updatedCountry.setChangeRequestId("CR-2024-001");
        Country savedUpdate = countryRepository.saveAndFlush(updatedCountry);
        
        // Then
        List<Country> allVersions = countryRepository.findAllVersionsByCountryCode("US");
        assertThat(allVersions).hasSize(2);
        
        // Verify the latest version is returned by default
        Optional<Country> current = countryRepository.findByCountryCodeAndCodeSystem("US", isoCodeSystem);
        assertThat(current).isPresent();
        assertThat(current.get().getCountryName()).isEqualTo("United States of America");
        assertThat(current.get().getVersion()).isEqualTo(2L);
        assertThat(current.get().getIsCorrection()).isTrue();
    }

    @Test
    @DisplayName("Should search countries by name pattern")
    void testSearchByNamePattern() {
        // Given
        createAndSaveCountries();
        countryRepository.saveAndFlush(createCountry("GB", "United Kingdom", isoCodeSystem));
        
        // When
        List<Country> unitedCountries = countryRepository.findByCountryNameContainingIgnoreCase("United");
        
        // Then
        assertThat(unitedCountries).hasSize(2);
        assertThat(unitedCountries)
                .extracting(Country::getCountryName)
                .containsExactlyInAnyOrder("United States", "United Kingdom");
    }

    @Test
    @DisplayName("Should handle database constraints correctly")
    void testDatabaseConstraints() {
        // Given
        Country country1 = createCountry("US", "United States", isoCodeSystem);
        countryRepository.saveAndFlush(country1);
        
        // When/Then - Should handle unique constraint on country_code + code_system
        Country duplicateCountry = createCountry("US", "United States Copy", isoCodeSystem);
        
        // This should work because we're creating a new version, not a duplicate
        duplicateCountry.setVersion(2L);
        duplicateCountry.setValidFrom(LocalDate.now().plusDays(1));
        
        // Should not throw exception
        countryRepository.saveAndFlush(duplicateCountry);
        
        List<Country> usVersions = countryRepository.findAllVersionsByCountryCode("US");
        assertThat(usVersions).hasSize(2);
    }

    @Test
    @DisplayName("Should cascade delete related entities properly")
    void testCascadeOperations() {
        // Given
        Country country = createCountry("US", "United States", isoCodeSystem);
        country.setMetadata("{\"test\": \"value\"}");
        Country savedCountry = countryRepository.saveAndFlush(country);
        
        // When
        countryRepository.delete(savedCountry);
        countryRepository.flush();
        
        // Then
        Optional<Country> deletedCountry = countryRepository.findById(savedCountry.getId());
        assertThat(deletedCountry).isEmpty();
        
        // Verify code system is not deleted (should not cascade)
        Optional<CodeSystem> codeSystem = codeSystemRepository.findById(isoCodeSystem.getId());
        assertThat(codeSystem).isPresent();
    }

    // Helper methods
    private Country createCountry(String code, String name, CodeSystem codeSystem) {
        Country country = new Country();
        // Let Hibernate manage ID
        country.setVersion(1L);
        country.setCodeSystem(codeSystem);
        country.setCountryCode(code);
        country.setCountryName(name);
        country.setValidFrom(LocalDate.now());
        country.setRecordedAt(LocalDateTime.now());
        country.setRecordedBy("test-user");
        country.setIsActive(true);
        
        // Set ISO codes for some common countries
        if ("US".equals(code)) {
            country.setIso2Code("US");
            country.setIso3Code("USA");
            country.setNumericCode("840");
        } else if ("CA".equals(code)) {
            country.setIso2Code("CA");
            country.setIso3Code("CAN");
            country.setNumericCode("124");
        } else if ("MX".equals(code)) {
            country.setIso2Code("MX");
            country.setIso3Code("MEX");
            country.setNumericCode("484");
        }
        
        return country;
    }
    
    private void createAndSaveCountries() {
        countryRepository.saveAndFlush(createCountry("US", "United States", isoCodeSystem));
        countryRepository.saveAndFlush(createCountry("CA", "Canada", isoCodeSystem));
        countryRepository.saveAndFlush(createCountry("MX", "Mexico", isoCodeSystem));
    }
}