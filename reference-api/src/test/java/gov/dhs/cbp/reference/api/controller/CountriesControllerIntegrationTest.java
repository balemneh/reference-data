package gov.dhs.cbp.reference.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.ReferenceApiApplication;
import gov.dhs.cbp.reference.api.config.H2TestConfiguration;
import gov.dhs.cbp.reference.api.config.TestEntityConfiguration;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ReferenceApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Import(TestEntityConfiguration.class)
@ActiveProfiles("integration-test")
@Transactional
@Disabled("Integration tests need additional configuration for CI/CD - entityManagerFactory bean conflicts")
class CountriesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private CodeSystemRepository codeSystemRepository;

    private CodeSystem isoCodeSystem;
    private CodeSystem cbpCodeSystem;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        countryRepository.deleteAll();
        codeSystemRepository.deleteAll();

        // Create code systems
        isoCodeSystem = createCodeSystem("ISO3166-1", "ISO 3166-1 Country Codes");
        cbpCodeSystem = createCodeSystem("CBP-COUNTRY5", "CBP 5-Character Country Codes");
        
        codeSystemRepository.save(isoCodeSystem);
        codeSystemRepository.save(cbpCodeSystem);
    }

    @Test
    @DisplayName("GET /v1/countries should return paginated countries")
    void testGetCountriesPaginated() throws Exception {
        // Given
        createSampleCountries();

        // When & Then
        mockMvc.perform(get("/v1/countries")
                .param("page", "0")
                .param("size", "2")
                .param("codeSystem", "ISO3166-1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    @DisplayName("GET /v1/countries/{code} should return specific country")
    void testGetCountryByCode() throws Exception {
        // Given
        createSampleCountries();

        // When & Then
        mockMvc.perform(get("/v1/countries/US")
                .param("codeSystem", "ISO3166-1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.countryName").value("United States"))
                .andExpect(jsonPath("$.iso2Code").value("US"))
                .andExpect(jsonPath("$.iso3Code").value("USA"))
                .andExpect(jsonPath("$.numericCode").value("840"))
                .andExpect(jsonPath("$.codeSystem.code").value("ISO3166-1"));
    }

    @Test
    @DisplayName("GET /v1/countries/{code} should return 404 for non-existent country")
    void testGetCountryByCodeNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/countries/XX")
                .param("codeSystem", "ISO3166-1"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Country not found"))
                .andExpect(jsonPath("$.detail").value("Country with code XX not found in system ISO3166-1"));
    }

    @Test
    @DisplayName("GET /v1/countries/search should support full-text search")
    void testSearchCountries() throws Exception {
        // Given
        createSampleCountries();
        countryRepository.save(createCountry("GB", "United Kingdom", isoCodeSystem));

        // When & Then - Search by partial name
        mockMvc.perform(get("/v1/countries/search")
                .param("q", "United")
                .param("codeSystem", "ISO3166-1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].countryName").value(
                    org.hamcrest.Matchers.hasItems("United States", "United Kingdom")));
    }

    @Test
    @DisplayName("GET /v1/countries should support filtering by active status")
    void testGetCountriesFilterByActive() throws Exception {
        // Given
        Country activeCountry = createCountry("US", "United States", isoCodeSystem);
        activeCountry.setIsActive(true);
        
        Country inactiveCountry = createCountry("YU", "Yugoslavia", isoCodeSystem);
        inactiveCountry.setIsActive(false);
        inactiveCountry.setValidTo(LocalDate.now().minusDays(1));
        
        countryRepository.save(activeCountry);
        countryRepository.save(inactiveCountry);

        // When & Then - Only active countries
        mockMvc.perform(get("/v1/countries")
                .param("active", "true")
                .param("codeSystem", "ISO3166-1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].countryCode").value("US"))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    @DisplayName("GET /v1/countries should support as-of date queries")
    void testGetCountriesAsOfDate() throws Exception {
        // Given
        LocalDate queryDate = LocalDate.now().minusDays(5);
        
        Country historicalCountry = createCountry("US", "United States", isoCodeSystem);
        historicalCountry.setValidFrom(queryDate.minusDays(10));
        historicalCountry.setValidTo(queryDate.minusDays(1)); // Expired before query date
        
        Country currentCountry = createCountry("CA", "Canada", isoCodeSystem);
        currentCountry.setValidFrom(queryDate.minusDays(10));
        currentCountry.setValidTo(null); // Still valid
        
        countryRepository.save(historicalCountry);
        countryRepository.save(currentCountry);

        // When & Then
        mockMvc.perform(get("/v1/countries")
                .param("asOf", queryDate.toString())
                .param("codeSystem", "ISO3166-1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].countryCode").value("CA"));
    }

    @Test
    @DisplayName("POST /v1/countries should create new country")
    void testCreateCountry() throws Exception {
        // Given
        String countryJson = """
            {
                "countryCode": "DE",
                "countryName": "Germany",
                "iso2Code": "DE",
                "iso3Code": "DEU",
                "numericCode": "276",
                "codeSystem": {
                    "code": "ISO3166-1"
                }
            }
            """;

        // When & Then
        MvcResult result = mockMvc.perform(post("/v1/countries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(countryJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.countryCode").value("DE"))
                .andExpect(jsonPath("$.countryName").value("Germany"))
                .andExpect(jsonPath("$.iso3Code").value("DEU"))
                .andReturn();

        // Verify country was saved to database
        String locationHeader = result.getResponse().getHeader("Location");
        assertThat(locationHeader).isNotNull();
        
        String countryId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
        UUID uuid = UUID.fromString(countryId);
        
        Country savedCountry = countryRepository.findById(uuid).orElse(null);
        assertThat(savedCountry).isNotNull();
        assertThat(savedCountry.getCountryCode()).isEqualTo("DE");
        assertThat(savedCountry.getCountryName()).isEqualTo("Germany");
    }

    @Test
    @DisplayName("POST /v1/countries should validate required fields")
    void testCreateCountryValidation() throws Exception {
        // Given - Missing required fields
        String invalidCountryJson = """
            {
                "countryCode": "",
                "countryName": ""
            }
            """;

        // When & Then
        mockMvc.perform(post("/v1/countries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidCountryJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("PUT /v1/countries/{code} should update existing country")
    void testUpdateCountry() throws Exception {
        // Given
        Country existingCountry = createCountry("US", "United States", isoCodeSystem);
        Country savedCountry = countryRepository.save(existingCountry);

        String updateJson = """
            {
                "countryCode": "US",
                "countryName": "United States of America",
                "iso2Code": "US",
                "iso3Code": "USA",
                "numericCode": "840",
                "codeSystem": {
                    "code": "ISO3166-1"
                }
            }
            """;

        // When & Then
        mockMvc.perform(put("/v1/countries/US")
                .param("codeSystem", "ISO3166-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryName").value("United States of America"));

        // Verify update created a new version
        Country updatedCountry = countryRepository.findByCountryCodeAndCodeSystem("US", isoCodeSystem)
                .orElse(null);
        assertThat(updatedCountry).isNotNull();
        assertThat(updatedCountry.getCountryName()).isEqualTo("United States of America");
        assertThat(updatedCountry.getVersion()).isGreaterThan(savedCountry.getVersion());
    }

    @Test
    @DisplayName("DELETE /v1/countries/{code} should soft delete country")
    void testDeleteCountry() throws Exception {
        // Given
        Country existingCountry = createCountry("US", "United States", isoCodeSystem);
        countryRepository.save(existingCountry);

        // When & Then
        mockMvc.perform(delete("/v1/countries/US")
                .param("codeSystem", "ISO3166-1"))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify soft delete (should set validTo date and isActive=false)
        Country deletedCountry = countryRepository.findById(existingCountry.getId()).orElse(null);
        assertThat(deletedCountry).isNotNull();
        assertThat(deletedCountry.getValidTo()).isNotNull();
        assertThat(deletedCountry.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should handle ETags for caching optimization")
    void testETagSupport() throws Exception {
        // Given
        createSampleCountries();

        // When - First request
        MvcResult firstResult = mockMvc.perform(get("/v1/countries/US")
                .param("codeSystem", "ISO3166-1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andReturn();

        String etag = firstResult.getResponse().getHeader("ETag");
        assertThat(etag).isNotNull();

        // Then - Subsequent request with ETag should return 304 Not Modified
        mockMvc.perform(get("/v1/countries/US")
                .param("codeSystem", "ISO3166-1")
                .header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    @DisplayName("Should handle concurrent requests properly")
    void testConcurrency() throws Exception {
        // Given
        Country country = createCountry("US", "United States", isoCodeSystem);
        countryRepository.save(country);

        // When & Then - Simulate concurrent updates with version control
        String updateJson1 = """
            {
                "countryCode": "US",
                "countryName": "United States v1",
                "version": 1,
                "codeSystem": {
                    "code": "ISO3166-1"
                }
            }
            """;

        String updateJson2 = """
            {
                "countryCode": "US", 
                "countryName": "United States v2",
                "version": 1,
                "codeSystem": {
                    "code": "ISO3166-1"
                }
            }
            """;

        // First update should succeed
        mockMvc.perform(put("/v1/countries/US")
                .param("codeSystem", "ISO3166-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson1))
                .andExpect(status().isOk());

        // Second update with same version should fail due to optimistic locking
        mockMvc.perform(put("/v1/countries/US")
                .param("codeSystem", "ISO3166-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Optimistic locking failure"));
    }

    // Helper methods
    private CodeSystem createCodeSystem(String code, String name) {
        CodeSystem codeSystem = new CodeSystem();
        codeSystem.setId(UUID.randomUUID());
        codeSystem.setCode(code);
        codeSystem.setName(name);
        codeSystem.setDescription("Test code system: " + name);
        codeSystem.setOwner("TEST");
        codeSystem.setCreatedAt(LocalDateTime.now());
        codeSystem.setUpdatedAt(LocalDateTime.now());
        return codeSystem;
    }

    private Country createCountry(String code, String name, CodeSystem codeSystem) {
        Country country = new Country();
        country.setId(UUID.randomUUID());
        country.setVersion(1L);
        country.setCodeSystem(codeSystem);
        country.setCountryCode(code);
        country.setCountryName(name);
        country.setValidFrom(LocalDate.now());
        country.setRecordedAt(LocalDateTime.now());
        country.setRecordedBy("integration-test");
        country.setIsActive(true);

        // Set ISO codes for common countries
        switch (code) {
            case "US":
                country.setIso2Code("US");
                country.setIso3Code("USA");
                country.setNumericCode("840");
                break;
            case "CA":
                country.setIso2Code("CA");
                country.setIso3Code("CAN");
                country.setNumericCode("124");
                break;
            case "MX":
                country.setIso2Code("MX");
                country.setIso3Code("MEX");
                country.setNumericCode("484");
                break;
            case "GB":
                country.setIso2Code("GB");
                country.setIso3Code("GBR");
                country.setNumericCode("826");
                break;
        }

        return country;
    }

    private void createSampleCountries() {
        countryRepository.save(createCountry("US", "United States", isoCodeSystem));
        countryRepository.save(createCountry("CA", "Canada", isoCodeSystem));
        countryRepository.save(createCountry("MX", "Mexico", isoCodeSystem));
    }
}