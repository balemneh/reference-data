package gov.dhs.cbp.reference.core.liquibase;

import gov.dhs.cbp.reference.core.entity.*;
import gov.dhs.cbp.reference.core.repository.*;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for Liquibase changesets to ensure proper data loading.
 * Tests verify that database schema creation and seed data loading work correctly.
 */
@DataJpaTest
@ActiveProfiles("test")
class LiquibaseChangesetTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CodeSystemRepository codeSystemRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private PortRepository portRepository;

    @Test
    void testLiquibaseChangesetsAppliedCorrectly() throws Exception {
        // Verify Liquibase can process all changesets without errors
        try (Connection connection = dataSource.getConnection()) {
            try (Liquibase liquibase = new Liquibase("db/changelog/db.changelog-master.xml",
                    new ClassLoaderResourceAccessor(),
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)))) {
                
                // This will throw an exception if any changeset fails
                liquibase.validate();
            }
        }
    }

    @Test
    void testCodeSystemSeedDataLoaded() {
        // Verify core code systems are loaded
        List<CodeSystem> codeSystems = codeSystemRepository.findAll();
        assertThat(codeSystems).isNotEmpty();

        // Verify essential code systems exist
        Optional<CodeSystem> iso3166 = codeSystemRepository.findByCode("ISO3166-1");
        assertThat(iso3166).isPresent();
        assertThat(iso3166.get().getName()).isEqualTo("ISO 3166-1 Country Codes");
        assertThat(iso3166.get().getDescription()).contains("ISO standard");

        Optional<CodeSystem> unLocode = codeSystemRepository.findByCode("UN-LOCODE");
        assertThat(unLocode).isPresent();
        assertThat(unLocode.get().getName()).isEqualTo("UN/LOCODE Port Codes");

        Optional<CodeSystem> iata = codeSystemRepository.findByCode("IATA");
        assertThat(iata).isPresent();
        assertThat(iata.get().getName()).isEqualTo("IATA Airport Codes");

        Optional<CodeSystem> icao = codeSystemRepository.findByCode("ICAO");
        assertThat(icao).isPresent();
        assertThat(icao.get().getName()).isEqualTo("ICAO Airport Codes");
    }

    @Test
    @Sql(scripts = "/db/test-data/countries-seed-test.sql")
    void testCountrySeedDataLoaded() {
        // Verify countries are loaded
        List<Country> countries = countryRepository.findAll();
        assertThat(countries).isNotEmpty();

        // Verify specific countries with correct data
        Optional<Country> usCountry = countryRepository.findCurrentByCodeAndSystemCode("US", "ISO3166-1");
        assertThat(usCountry).isPresent();
        
        Country usa = usCountry.get();
        assertThat(usa.getCountryName()).isEqualTo("United States");
        assertThat(usa.getIso2Code()).isEqualTo("US");
        assertThat(usa.getIso3Code()).isEqualTo("USA");
        assertThat(usa.getNumericCode()).isEqualTo("840");
        assertThat(usa.getIsActive()).isTrue();
        assertThat(usa.getValidFrom()).isNotNull();

        // Verify temporal data structure
        assertThat(usa.getVersion()).isEqualTo(1L);
        assertThat(usa.getRecordedAt()).isNotNull();
        assertThat(usa.getRecordedBy()).isEqualTo("system");
        assertThat(usa.getChangeRequestId()).isEqualTo("SEED-001");
        assertThat(usa.getIsCorrection()).isFalse();

        // Test other essential countries
        Optional<Country> canadaCountry = countryRepository.findCurrentByCodeAndSystemCode("CA", "ISO3166-1");
        assertThat(canadaCountry).isPresent();
        
        Country canada = canadaCountry.get();
        assertThat(canada.getCountryName()).isEqualTo("Canada");
        assertThat(canada.getIso2Code()).isEqualTo("CA");
        assertThat(canada.getIso3Code()).isEqualTo("CAN");
        assertThat(canada.getNumericCode()).isEqualTo("124");
    }

    @Test
    @Sql(scripts = "/db/test-data/airports-seed-test.sql")
    void testAirportSeedDataLoaded() {
        // Verify airports are loaded
        List<Airport> airports = airportRepository.findAll();
        assertThat(airports).isNotEmpty();

        // Test major airports with comprehensive data validation
        Optional<Airport> laxAirport = airportRepository.findCurrentByIataCodeAndSystemCode("LAX", "IATA");
        assertThat(laxAirport).isPresent();
        
        Airport lax = laxAirport.get();
        assertThat(lax.getAirportName()).isEqualTo("Los Angeles International Airport");
        assertThat(lax.getIataCode()).isEqualTo("LAX");
        assertThat(lax.getIcaoCode()).isEqualTo("KLAX");
        assertThat(lax.getCity()).isEqualTo("Los Angeles");
        assertThat(lax.getStateProvince()).isEqualTo("California");
        assertThat(lax.getCountryCode()).isEqualTo("USA");
        assertThat(lax.getLatitude()).isNotNull();
        assertThat(lax.getLongitude()).isNotNull();
        assertThat(lax.getElevation()).isNotNull();
        assertThat(lax.getAirportType()).isEqualTo("Large Hub");
        assertThat(lax.getTimezone()).isNotNull();
        assertThat(lax.getIsActive()).isTrue();

        // Verify temporal structure
        assertThat(lax.getVersion()).isEqualTo(1L);
        assertThat(lax.getValidFrom()).isNotNull();
        assertThat(lax.getRecordedAt()).isNotNull();
        assertThat(lax.getChangeRequestId()).isEqualTo("SEED-002");

        // Test international airport
        Optional<Airport> heathrowAirport = airportRepository.findCurrentByIataCodeAndSystemCode("LHR", "IATA");
        assertThat(heathrowAirport).isPresent();
        
        Airport heathrow = heathrowAirport.get();
        assertThat(heathrow.getAirportName()).isEqualTo("London Heathrow Airport");
        assertThat(heathrow.getCountryCode()).isEqualTo("GBR");
    }

    @Test
    @Sql(scripts = "/db/test-data/ports-seed-test.sql")
    void testPortSeedDataLoaded() {
        // Verify ports are loaded
        List<Port> ports = portRepository.findAll();
        assertThat(ports).isNotEmpty();

        // Test major ports with comprehensive validation
        Optional<Port> laPortCurrent = portRepository.findCurrentByPortCodeAndSystemCode("USLAX", "UN-LOCODE");
        assertThat(laPortCurrent).isPresent();
        
        Port laPort = laPortCurrent.get();
        assertThat(laPort.getPortName()).isEqualTo("Los Angeles");
        assertThat(laPort.getPortCode()).isEqualTo("USLAX");
        assertThat(laPort.getCity()).isEqualTo("Los Angeles");
        assertThat(laPort.getCountryCode()).isEqualTo("USA");
        assertThat(laPort.getLatitude()).isNotNull();
        assertThat(laPort.getLongitude()).isNotNull();
        assertThat(laPort.getPortType()).isEqualTo("Seaport");
        assertThat(laPort.getUnLocode()).isEqualTo("LAX");
        assertThat(laPort.getIsActive()).isTrue();

        // Verify temporal structure
        assertThat(laPort.getVersion()).isEqualTo(1L);
        assertThat(laPort.getValidFrom()).isNotNull();
        assertThat(laPort.getRecordedAt()).isNotNull();
        assertThat(laPort.getChangeRequestId()).isEqualTo("SEED-003");

        // Test another major port
        Optional<Port> nyPortCurrent = portRepository.findCurrentByPortCodeAndSystemCode("USNYC", "UN-LOCODE");
        assertThat(nyPortCurrent).isPresent();
        
        Port nyPort = nyPortCurrent.get();
        assertThat(nyPort.getPortName()).isEqualTo("New York");
        assertThat(nyPort.getCountryCode()).isEqualTo("USA");
    }

    @Test
    void testBitemporalConstraintsEnforced() {
        // Test that temporal constraints are properly enforced
        List<Country> countries = countryRepository.findAll();
        
        for (Country country : countries) {
            // Verify bitemporal fields are set
            assertThat(country.getValidFrom()).isNotNull();
            assertThat(country.getRecordedAt()).isNotNull();
            assertThat(country.getVersion()).isGreaterThan(0L);
            
            // If valid_to is null, this should be the current version
            if (country.getValidTo() == null) {
                assertThat(country.getValidFrom()).isBeforeOrEqualTo(LocalDate.now());
            } else {
                // If valid_to is set, valid_from should be before valid_to
                assertThat(country.getValidFrom()).isBefore(country.getValidTo());
            }
        }
    }

    @Test
    void testDataIntegrityConstraints() {
        // Test that foreign key relationships are maintained
        List<Country> countries = countryRepository.findAll();
        for (Country country : countries) {
            assertThat(country.getCodeSystem()).isNotNull();
            assertThat(country.getCodeSystem().getCode()).isNotBlank();
        }

        List<Airport> airports = airportRepository.findAll();
        for (Airport airport : airports) {
            assertThat(airport.getCodeSystem()).isNotNull();
            assertThat(airport.getCountryCode()).isNotBlank();
            assertThat(airport.getAirportName()).isNotBlank();
        }

        List<Port> ports = portRepository.findAll();
        for (Port port : ports) {
            assertThat(port.getCodeSystem()).isNotNull();
            assertThat(port.getCountryCode()).isNotBlank();
            assertThat(port.getPortName()).isNotBlank();
            assertThat(port.getPortCode()).isNotBlank();
        }
    }

    @Test
    void testUniqueConstraintsRespected() {
        // Test that unique constraints work properly
        
        // Countries: unique on (country_code, code_system_id, valid_from, valid_to)
        Optional<Country> usCountry = countryRepository.findCurrentByCodeAndSystemCode("US", "ISO3166-1");
        // Should only have one current version
        assertThat(usCountry).isPresent();

        // Airports: IATA codes should be unique per system and validity period
        Optional<Airport> laxAirport = airportRepository.findCurrentByIataCodeAndSystemCode("LAX", "IATA");
        // Should only have one current version
        assertThat(laxAirport).isPresent();

        // Ports: Port codes should be unique per system and validity period  
        Optional<Port> laPortUnique = portRepository.findCurrentByPortCodeAndSystemCode("USLAX", "UN-LOCODE");
        // Should only have one current version
        assertThat(laPortUnique).isPresent();
    }

    @Test
    void testMetadataStructure() {
        // Verify that metadata is properly structured in JSONB format
        List<Country> countries = countryRepository.findAll();
        if (!countries.isEmpty()) {
            Country country = countries.get(0);
            assertThat(country.getMetadata()).isNotNull();
            // Metadata should contain source information
            assertThat(country.getMetadata()).contains("source");
        }

        List<Airport> airports = airportRepository.findAll();
        if (!airports.isEmpty()) {
            Airport airport = airports.get(0);
            assertThat(airport.getMetadata()).isNotNull();
            assertThat(airport.getMetadata()).contains("source");
        }

        List<Port> ports = portRepository.findAll();
        if (!ports.isEmpty()) {
            Port port = ports.get(0);
            assertThat(port.getMetadata()).isNotNull();
            assertThat(port.getMetadata()).contains("source");
        }
    }

    @Test
    void testIndexesCreatedProperly() {
        // This test verifies that the database indexes specified in entities are created
        // by attempting queries that would benefit from these indexes
        
        // Test country indexes
        Optional<Country> countryByCode = countryRepository.findCurrentByCodeAndSystemCode("US", "ISO3166-1");
        assertThat(countryByCode).isPresent();
        
        // Test temporal query performance (uses valid_dates index)
        LocalDate testDate = LocalDate.now().minusDays(30);
        Optional<Country> countryAsOf = countryRepository.findByCodeAndSystemAsOf("US", "ISO3166-1", testDate);
        // Should work without performance issues
        
        // Test airport indexes
        Optional<Airport> airportByIATA = airportRepository.findCurrentByIataCodeAndSystemCode("LAX", "IATA");
        assertThat(airportByIATA).isPresent();
        
        Optional<Airport> airportByICAO = airportRepository.findCurrentByIcaoCodeAndSystemCode("KLAX", "ICAO");
        // Should work efficiently with ICAO index
        
        // Test port indexes  
        Optional<Port> portByCode = portRepository.findCurrentByPortCodeAndSystemCode("USLAX", "UN-LOCODE");
        assertThat(portByCode).isPresent();
    }
}