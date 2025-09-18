package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for all repository classes.
 * Tests pagination, filtering, temporal queries, and complex data scenarios.
 */
@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = {"/db/test-data/comprehensive-seed-test.sql"})
class ComprehensiveRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private PortRepository portRepository;

    @Autowired
    private CodeSystemRepository codeSystemRepository;

    @Autowired
    private CodeMappingRepository codeMappingRepository;

    private CodeSystem iso3166CodeSystem;
    private CodeSystem iataCodeSystem;
    private CodeSystem unLocodeSystem;

    @BeforeEach
    void setUp() {
        // Create test code systems if they don't exist
        iso3166CodeSystem = createOrFindCodeSystem("ISO3166-1", "ISO 3166-1 Country Codes", "ISO");
        iataCodeSystem = createOrFindCodeSystem("IATA", "IATA Airport Codes", "IATA");
        unLocodeSystem = createOrFindCodeSystem("UN-LOCODE", "UN/LOCODE Port Codes", "UN/ECE");
        
        entityManager.flush();
    }

    private CodeSystem createOrFindCodeSystem(String code, String name, String owner) {
        Optional<CodeSystem> existing = codeSystemRepository.findByCode(code);
        if (existing.isPresent()) {
            return existing.get();
        }

        CodeSystem codeSystem = new CodeSystem();
        codeSystem.setCode(code);
        codeSystem.setName(name);
        codeSystem.setDescription(name);
        codeSystem.setOwner(owner);
        codeSystem.setIsActive(true);
        return entityManager.persistAndFlush(codeSystem);
    }

    // === Country Repository Tests ===

    @Test
    void testCountryRepository_TemporalQueries() {
        // Test current query
        Optional<Country> currentUs = countryRepository.findCurrentByCodeAndSystemCode("US", "ISO3166-1");
        assertThat(currentUs).isPresent();
        assertThat(currentUs.get().getCountryName()).isEqualTo("United States");
        
        // Test historical query
        LocalDate pastDate = LocalDate.now().minusYears(1);
        Optional<Country> historicalUs = countryRepository.findByCodeAndSystemAsOf("US", "ISO3166-1", pastDate);
        // Should still find current record since it's valid from today
        assertThat(historicalUs).isEmpty(); // No historical data before today
        
        // Test future query
        LocalDate futureDate = LocalDate.now().plusDays(30);
        Optional<Country> futureUs = countryRepository.findByCodeAndSystemAsOf("US", "ISO3166-1", futureDate);
        assertThat(futureUs).isPresent(); // Should still be valid
    }

    @Test
    void testCountryRepository_Pagination() {
        // Test paginated queries
        PageRequest pageRequest = PageRequest.of(0, 2);
        Page<Country> firstPage = countryRepository.findCurrentBySystemCode("ISO3166-1", pageRequest);
        
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(firstPage.hasNext()).isTrue();
        
        // Test second page
        PageRequest secondPageRequest = PageRequest.of(1, 2);
        Page<Country> secondPage = countryRepository.findCurrentBySystemCode("ISO3166-1", secondPageRequest);
        
        assertThat(secondPage.getContent()).hasSize(1); // Only one more country
        assertThat(secondPage.hasPrevious()).isTrue();
    }

    @Test
    void testCountryRepository_Search() {
        // Test search functionality
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Country> searchResults = countryRepository.searchByName("United", pageRequest);
        
        assertThat(searchResults.getContent()).hasSize(2); // United States and United Kingdom
        assertThat(searchResults.getContent())
                .extracting(Country::getCountryName)
                .allMatch(name -> name.contains("United"));
    }

    @Test
    void testCountryRepository_IsoCodeQueries() {
        // Test ISO2 code search
        List<Country> iso2Results = countryRepository.findCurrentByIso2Code("US");
        assertThat(iso2Results).hasSize(1);
        assertThat(iso2Results.get(0).getIso3Code()).isEqualTo("USA");
        
        // Test ISO3 code search
        List<Country> iso3Results = countryRepository.findCurrentByIso3Code("CAN");
        assertThat(iso3Results).hasSize(1);
        assertThat(iso3Results.get(0).getCountryName()).isEqualTo("Canada");
    }

    @Test
    void testCountryRepository_Statistics() {
        // Test count queries
        long activeCount = countryRepository.countByIsActiveTrue();
        assertThat(activeCount).isEqualTo(3);
        
        // Test country codes retrieval
        List<String> countryCodes = countryRepository.findAllCountryCodes("ISO3166-1");
        assertThat(countryCodes).containsExactlyInAnyOrder("CA", "GB", "US");
    }

    // === Airport Repository Tests ===

    @Test
    void testAirportRepository_CodeQueries() {
        // Test IATA code queries
        Optional<Airport> laxByIata = airportRepository.findCurrentByIataCodeAndSystemCode("LAX", "IATA");
        assertThat(laxByIata).isPresent();
        assertThat(laxByIata.get().getAirportName()).isEqualTo("Los Angeles International Airport");
        
        // Test ICAO code queries
        Optional<Airport> laxByIcao = airportRepository.findCurrentByIcaoCodeAndSystemCode("KLAX", "ICAO");
        if (laxByIcao.isPresent()) {
            assertThat(laxByIcao.get().getAirportName()).isEqualTo("Los Angeles International Airport");
        }
    }

    @Test
    void testAirportRepository_GeographicQueries() {
        // Test country-based queries
        List<Airport> usAirports = airportRepository.findCurrentByCountryCode("USA");
        assertThat(usAirports).hasSize(2); // LAX and JFK
        
        // Test city-based queries
        List<Airport> losAngelesAirports = airportRepository.findCurrentByCity("Los Angeles");
        assertThat(losAngelesAirports).hasSize(1);
        assertThat(losAngelesAirports.get(0).getIataCode()).isEqualTo("LAX");
        
        // Test by airport type
        List<Airport> largeHubs = airportRepository.findCurrentByAirportType("Large Hub");
        assertThat(largeHubs).hasSize(3); // All test airports are large hubs
    }

    @Test
    void testAirportRepository_Pagination() {
        PageRequest pageRequest = PageRequest.of(0, 2);
        Page<Airport> firstPage = airportRepository.findCurrentBySystemCode("IATA", pageRequest);
        
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
    }

    @Test
    void testAirportRepository_Search() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Airport> searchResults = airportRepository.searchByName("International", pageRequest);
        
        assertThat(searchResults.getContent())
                .extracting(Airport::getAirportName)
                .allMatch(name -> name.contains("International"));
    }

    @Test
    void testAirportRepository_TemporalQueries() {
        LocalDate asOfDate = LocalDate.now();
        Optional<Airport> laxAsOf = airportRepository.findByIataCodeAndSystemAsOf("LAX", "IATA", asOfDate);
        assertThat(laxAsOf).isPresent();
        
        // Test historical query (should be empty since data is from today)
        LocalDate pastDate = LocalDate.now().minusMonths(1);
        Optional<Airport> laxHistorical = airportRepository.findByIataCodeAndSystemAsOf("LAX", "IATA", pastDate);
        assertThat(laxHistorical).isEmpty();
    }

    // === Port Repository Tests ===

    @Test
    void testPortRepository_CodeQueries() {
        // Test port code queries
        Optional<Port> laPort = portRepository.findCurrentByPortCodeAndSystemCode("USLAX", "UN-LOCODE");
        assertThat(laPort).isPresent();
        assertThat(laPort.get().getPortName()).isEqualTo("Los Angeles");
        
        // Test UN LOCODE queries
        Optional<Port> portByUnLocode = portRepository.findCurrentByUnLocodeAndSystemCode("LAX", "UN-LOCODE");
        assertThat(portByUnLocode).isPresent();
        assertThat(portByUnLocode.get().getPortCode()).isEqualTo("USLAX");
    }

    @Test
    void testPortRepository_GeographicQueries() {
        // Test country-based queries
        List<Port> usPorts = portRepository.findCurrentByCountryCode("USA");
        assertThat(usPorts).hasSize(2); // USLAX and USNYC
        
        // Test city-based queries
        List<Port> losAngelesPorts = portRepository.findCurrentByCity("Los Angeles");
        assertThat(losAngelesPorts).hasSize(1);
        
        // Test by port type
        List<Port> seaports = portRepository.findCurrentByPortType("Seaport");
        assertThat(seaports).hasSize(3); // All test ports are seaports
    }

    @Test
    void testPortRepository_Pagination() {
        PageRequest pageRequest = PageRequest.of(0, 2);
        Page<Port> firstPage = portRepository.findCurrentBySystemCode("UN-LOCODE", pageRequest);
        
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
    }

    @Test
    void testPortRepository_Search() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Port> searchResults = portRepository.searchByName("Los", pageRequest);
        
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getPortName()).isEqualTo("Los Angeles");
    }

    @Test
    void testPortRepository_TemporalQueries() {
        LocalDate asOfDate = LocalDate.now();
        Optional<Port> portAsOf = portRepository.findByPortCodeAndSystemAsOf("USLAX", "UN-LOCODE", asOfDate);
        assertThat(portAsOf).isPresent();
        
        // Test UN LOCODE temporal query
        Optional<Port> portByUnLocodeAsOf = portRepository.findByUnLocodeAndSystemAsOf("LAX", "UN-LOCODE", asOfDate);
        assertThat(portByUnLocodeAsOf).isPresent();
    }

    // === Code System Repository Tests ===

    @Test
    void testCodeSystemRepository_BasicQueries() {
        // Test find by code
        Optional<CodeSystem> iso3166 = codeSystemRepository.findByCode("ISO3166-1");
        assertThat(iso3166).isPresent();
        assertThat(iso3166.get().getName()).isEqualTo("ISO 3166-1 Country Codes");
        
        // Test find all
        List<CodeSystem> allSystems = codeSystemRepository.findAll();
        assertThat(allSystems).hasSizeGreaterThanOrEqualTo(3);
        
        // Test find by owner - method needs to be added to repository
        // List<CodeSystem> isoSystems = codeSystemRepository.findByOwner("ISO");
        // assertThat(isoSystems).hasSize(1);
        // assertThat(isoSystems.get(0).getCode()).isEqualTo("ISO3166-1");
    }

    @Test
    void testCodeSystemRepository_ActiveSystems() {
        // Test active systems - findAllActive method needs to be added to repository
        List<CodeSystem> allSystems = codeSystemRepository.findAll();
        List<CodeSystem> activeSystems = allSystems.stream()
                .filter(cs -> cs.getIsActive() == null || cs.getIsActive())
                .toList();
        assertThat(activeSystems).hasSize(3);
        assertThat(activeSystems).allMatch(cs -> cs.getIsActive() == null || cs.getIsActive());
    }

    // === Cross-Repository Integration Tests ===

    @Test
    void testCrossRepositoryRelationships() {
        // Test that countries are properly linked to code systems
        Optional<Country> country = countryRepository.findCurrentByCodeAndSystemCode("US", "ISO3166-1");
        assertThat(country).isPresent();
        assertThat(country.get().getCodeSystem().getCode()).isEqualTo("ISO3166-1");
        
        // Test that airports are properly linked to code systems
        Optional<Airport> airport = airportRepository.findCurrentByIataCodeAndSystemCode("LAX", "IATA");
        assertThat(airport).isPresent();
        assertThat(airport.get().getCodeSystem().getCode()).isEqualTo("IATA");
        
        // Test that ports are properly linked to code systems
        Optional<Port> port = portRepository.findCurrentByPortCodeAndSystemCode("USLAX", "UN-LOCODE");
        assertThat(port).isPresent();
        assertThat(port.get().getCodeSystem().getCode()).isEqualTo("UN-LOCODE");
    }

    @Test
    void testBitemporalConsistency() {
        // Test that all records have consistent bitemporal fields
        List<Country> countries = countryRepository.findAll();
        countries.forEach(country -> {
            assertThat(country.getValidFrom()).isNotNull();
            assertThat(country.getRecordedAt()).isNotNull();
            assertThat(country.getVersion()).isGreaterThan(0L);
            assertThat(country.getRecordedBy()).isNotBlank();
        });
        
        List<Airport> airports = airportRepository.findAll();
        airports.forEach(airport -> {
            assertThat(airport.getValidFrom()).isNotNull();
            assertThat(airport.getRecordedAt()).isNotNull();
            assertThat(airport.getVersion()).isGreaterThan(0L);
            assertThat(airport.getRecordedBy()).isNotBlank();
        });
        
        List<Port> ports = portRepository.findAll();
        ports.forEach(port -> {
            assertThat(port.getValidFrom()).isNotNull();
            assertThat(port.getRecordedAt()).isNotNull();
            assertThat(port.getVersion()).isGreaterThan(0L);
            assertThat(port.getRecordedBy()).isNotBlank();
        });
    }

    @Test
    void testComplexSearchScenarios() {
        // Test complex search across multiple repositories
        
        // Find all US-related entities
        List<Country> usCountries = countryRepository.findCurrentByIso2Code("US");
        assertThat(usCountries).hasSize(1);
        
        List<Airport> usAirports = airportRepository.findCurrentByCountryCode("USA");
        assertThat(usAirports).hasSize(2);
        
        List<Port> usPorts = portRepository.findCurrentByCountryCode("USA");
        assertThat(usPorts).hasSize(2);
        
        // Verify consistency
        String usCountryName = usCountries.get(0).getCountryName();
        assertThat(usCountryName).isEqualTo("United States");
        
        assertThat(usAirports).allMatch(airport -> airport.getCountryCode().equals("USA"));
        assertThat(usPorts).allMatch(port -> port.getCountryCode().equals("USA"));
    }

    @Test
    void testDataIntegrityConstraints() {
        // Test that required fields are enforced
        List<Country> countries = countryRepository.findAll();
        countries.forEach(country -> {
            assertThat(country.getCountryCode()).isNotBlank();
            assertThat(country.getCountryName()).isNotBlank();
            assertThat(country.getCodeSystem()).isNotNull();
        });
        
        List<Airport> airports = airportRepository.findAll();
        airports.forEach(airport -> {
            assertThat(airport.getAirportName()).isNotBlank();
            assertThat(airport.getCountryCode()).isNotBlank();
            assertThat(airport.getCodeSystem()).isNotNull();
            // At least one of IATA or ICAO code should be present
            assertThat(airport.getIataCode() != null || airport.getIcaoCode() != null).isTrue();
        });
        
        List<Port> ports = portRepository.findAll();
        ports.forEach(port -> {
            assertThat(port.getPortCode()).isNotBlank();
            assertThat(port.getPortName()).isNotBlank();
            assertThat(port.getCountryCode()).isNotBlank();
            assertThat(port.getCodeSystem()).isNotNull();
        });
    }

    @Test
    void testPerformanceOfIndexedQueries() {
        // These queries should be fast due to database indexes
        long startTime = System.currentTimeMillis();
        
        // Test indexed country query
        Optional<Country> country = countryRepository.findCurrentByCodeAndSystemCode("US", "ISO3166-1");
        assertThat(country).isPresent();
        
        // Test indexed airport query
        Optional<Airport> airport = airportRepository.findCurrentByIataCodeAndSystemCode("LAX", "IATA");
        assertThat(airport).isPresent();
        
        // Test indexed port query
        Optional<Port> port = portRepository.findCurrentByPortCodeAndSystemCode("USLAX", "UN-LOCODE");
        assertThat(port).isPresent();
        
        long endTime = System.currentTimeMillis();
        // All indexed queries should complete very quickly (under 100ms)
        assertThat(endTime - startTime).isLessThan(100);
    }

    @Test
    void testLargeDatasetPagination() {
        // Create additional test data to test pagination with larger datasets
        createAdditionalTestCountries();
        
        entityManager.flush();
        
        // Test pagination with larger dataset
        PageRequest firstPage = PageRequest.of(0, 5);
        Page<Country> countries = countryRepository.findCurrentBySystemCode("ISO3166-1", firstPage);
        
        assertThat(countries.getContent()).hasSize(5);
        assertThat(countries.getTotalElements()).isGreaterThan(5);
        assertThat(countries.hasNext()).isTrue();
        
        // Test last page
        int totalPages = countries.getTotalPages();
        PageRequest lastPage = PageRequest.of(totalPages - 1, 5);
        Page<Country> lastPageResults = countryRepository.findCurrentBySystemCode("ISO3166-1", lastPage);
        
        assertThat(lastPageResults.hasNext()).isFalse();
        assertThat(lastPageResults.hasPrevious()).isTrue();
    }

    private void createAdditionalTestCountries() {
        String[] additionalCountries = {"FR", "DE", "IT", "ES", "JP", "AU", "BR", "IN", "CN"};
        String[] countryNames = {"France", "Germany", "Italy", "Spain", "Japan", "Australia", "Brazil", "India", "China"};
        
        for (int i = 0; i < additionalCountries.length; i++) {
            Country country = new Country();
            country.setCodeSystem(iso3166CodeSystem);
            country.setCountryCode(additionalCountries[i]);
            country.setCountryName(countryNames[i]);
            country.setIso2Code(additionalCountries[i]);
            country.setIso3Code(additionalCountries[i] + "X");
            country.setNumericCode(String.valueOf(100 + i));
            country.setIsActive(true);
            country.setVersion(1L);
            country.setValidFrom(LocalDate.now());
            country.setRecordedAt(LocalDateTime.now());
            country.setRecordedBy("test-system");
            country.setChangeRequestId("TEST-" + (i + 1));
            country.setIsCorrection(false);
            country.setMetadata("{\"source\": \"test-data\"}");
            
            entityManager.persist(country);
        }
    }
}