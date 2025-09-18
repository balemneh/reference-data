package gov.dhs.cbp.reference.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.config.WebMvcTestConfig;
import gov.dhs.cbp.reference.api.dto.AirportDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.service.AirportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Comprehensive unit tests for AirportsController.
 * Tests all endpoints with various scenarios including edge cases and error conditions.
 */
@WebMvcTest(controllers = AirportsController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
class AirportsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AirportService airportService;

    @Autowired
    private ObjectMapper objectMapper;

    private AirportDto sampleAirport;

    @BeforeEach
    void setUp() {
        sampleAirport = createSampleAirport();
    }

    private AirportDto createSampleAirport() {
        AirportDto airport = new AirportDto();
        airport.setId(UUID.randomUUID());
        airport.setIataCode("LAX");
        airport.setIcaoCode("KLAX");
        airport.setAirportName("Los Angeles International Airport");
        airport.setCity("Los Angeles");
        airport.setStateProvince("California");
        airport.setCountryCode("USA");
        airport.setLatitude(BigDecimal.valueOf(33.9425));
        airport.setLongitude(BigDecimal.valueOf(-118.4081));
        airport.setElevation(125);
        airport.setAirportType("Large Hub");
        airport.setTimezone("America/Los_Angeles");
        airport.setCodeSystem("IATA");
        airport.setIsActive(true);
        airport.setValidFrom(LocalDate.now());
        return airport;
    }

    // GET /{id} tests
    @Test
    void testGetAirportById_Success() throws Exception {
        UUID id = sampleAirport.getId();
        when(airportService.findById(id)).thenReturn(Optional.of(sampleAirport));

        mockMvc.perform(get("/v1/airports/{id}", id))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("LAX"))
                .andExpect(jsonPath("$.airportName").value("Los Angeles International Airport"))
                .andExpect(jsonPath("$.city").value("Los Angeles"))
                .andExpect(jsonPath("$.countryCode").value("USA"));
    }

    @Test
    void testGetAirportById_NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(airportService.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/airports/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAirportById_InvalidUuid() throws Exception {
        mockMvc.perform(get("/v1/airports/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    // GET / (by system code) tests
    @Test
    void testGetAirportsBySystemCode_Success() throws Exception {
        List<AirportDto> airports = Arrays.asList(sampleAirport);
        PagedResponse<AirportDto> response = new PagedResponse<>(airports, 0, 20, 1);
        
        when(airportService.findBySystemCode(eq("IATA"), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/v1/airports")
                .param("codeSystem", "IATA")
                .param("page", "0")
                .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].iataCode").value("LAX"))
                .andExpect(jsonPath("$.content[0].airportName").value("Los Angeles International Airport"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetAirportsBySystemCode_EmptyResult() throws Exception {
        PagedResponse<AirportDto> emptyResponse = new PagedResponse<>(Collections.emptyList(), 0, 20, 0);
        
        when(airportService.findBySystemCode(eq("ICAO"), any(PageRequest.class))).thenReturn(emptyResponse);

        mockMvc.perform(get("/v1/airports")
                .param("codeSystem", "ICAO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testGetAirportsBySystemCode_MissingParameter() throws Exception {
        mockMvc.perform(get("/v1/airports"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAirportsBySystemCode_CustomPagination() throws Exception {
        List<AirportDto> airports = Arrays.asList(sampleAirport);
        PagedResponse<AirportDto> response = new PagedResponse<>(airports, 2, 5, 11);
        
        when(airportService.findBySystemCode(eq("IATA"), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/v1/airports")
                .param("codeSystem", "IATA")
                .param("page", "2")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(11));
    }

    // GET /search tests
    @Test
    void testSearchAirports_Success() throws Exception {
        List<AirportDto> airports = Arrays.asList(sampleAirport);
        PagedResponse<AirportDto> response = new PagedResponse<>(airports, 0, 20, 1);
        
        when(airportService.searchByNameCityCountry(eq("Los Angeles"), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/v1/airports/search")
                .param("query", "Los Angeles"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].airportName").value("Los Angeles International Airport"));
    }

    @Test
    void testSearchAirports_MinQueryLength() throws Exception {
        List<AirportDto> airports = Arrays.asList(sampleAirport);
        PagedResponse<AirportDto> response = new PagedResponse<>(airports, 0, 20, 1);
        
        when(airportService.searchByNameCityCountry(eq("LA"), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/v1/airports/search")
                .param("query", "LA"))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchAirports_QueryTooShort() throws Exception {
        mockMvc.perform(get("/v1/airports/search")
                .param("query", "L"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchAirports_QueryTooLong() throws Exception {
        String longQuery = "A".repeat(256);
        mockMvc.perform(get("/v1/airports/search")
                .param("query", longQuery))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchAirports_MissingQuery() throws Exception {
        mockMvc.perform(get("/v1/airports/search"))
                .andExpect(status().isBadRequest());
    }

    // GET /by-country tests
    @Test
    void testGetAirportsByCountry_Success() throws Exception {
        List<AirportDto> airports = Arrays.asList(sampleAirport);
        when(airportService.findByCountryCode("USA")).thenReturn(airports);

        mockMvc.perform(get("/v1/airports/by-country")
                .param("countryCode", "USA"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].countryCode").value("USA"))
                .andExpect(jsonPath("$[0].airportName").value("Los Angeles International Airport"));
    }

    @Test
    void testGetAirportsByCountry_EmptyResult() throws Exception {
        when(airportService.findByCountryCode("ZZZ")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/airports/by-country")
                .param("countryCode", "ZZZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetAirportsByCountry_InvalidLength() throws Exception {
        mockMvc.perform(get("/v1/airports/by-country")
                .param("countryCode", "US"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/v1/airports/by-country")
                .param("countryCode", "USAA"))
                .andExpect(status().isBadRequest());
    }

    // GET /by-city tests
    @Test
    void testGetAirportsByCity_Success() throws Exception {
        List<AirportDto> airports = Arrays.asList(sampleAirport);
        when(airportService.findByCity("Los Angeles")).thenReturn(airports);

        mockMvc.perform(get("/v1/airports/by-city")
                .param("city", "Los Angeles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Los Angeles"));
    }

    @Test
    void testGetAirportsByCity_MinLength() throws Exception {
        when(airportService.findByCity("LA")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/airports/by-city")
                .param("city", "LA"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAirportsByCity_TooShort() throws Exception {
        mockMvc.perform(get("/v1/airports/by-city")
                .param("city", "L"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAirportsByCity_TooLong() throws Exception {
        String longCity = "A".repeat(101);
        mockMvc.perform(get("/v1/airports/by-city")
                .param("city", longCity))
                .andExpect(status().isBadRequest());
    }

    // GET /by-type tests
    @Test
    void testGetAirportsByType_Success() throws Exception {
        List<AirportDto> airports = Arrays.asList(sampleAirport);
        when(airportService.findByAirportType("Large Hub")).thenReturn(airports);

        mockMvc.perform(get("/v1/airports/by-type")
                .param("airportType", "Large Hub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].airportType").value("Large Hub"));
    }

    @Test
    void testGetAirportsByType_International() throws Exception {
        List<AirportDto> airports = Arrays.asList(sampleAirport);
        when(airportService.findByAirportType("International")).thenReturn(airports);

        mockMvc.perform(get("/v1/airports/by-type")
                .param("airportType", "International"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAirportsByType_ValidationError() throws Exception {
        mockMvc.perform(get("/v1/airports/by-type")
                .param("airportType", "X"))
                .andExpect(status().isBadRequest());

        String longType = "A".repeat(51);
        mockMvc.perform(get("/v1/airports/by-type")
                .param("airportType", longType))
                .andExpect(status().isBadRequest());
    }

    // GET /current tests
    @Test
    void testGetAllCurrentAirports_Success() throws Exception {
        List<AirportDto> airports = Arrays.asList(sampleAirport);
        when(airportService.findAllCurrent()).thenReturn(airports);

        mockMvc.perform(get("/v1/airports/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void testGetAllCurrentAirports_EmptyResult() throws Exception {
        when(airportService.findAllCurrent()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/airports/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // POST / (create) tests
    @Test
    void testCreateAirport_Success() throws Exception {
        AirportDto newAirport = createSampleAirport();
        newAirport.setId(null); // New airport shouldn't have ID
        
        AirportDto createdAirport = createSampleAirport();
        when(airportService.save(any(AirportDto.class))).thenReturn(createdAirport);

        mockMvc.perform(post("/v1/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAirport)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.iataCode").value("LAX"));
    }

    @Test
    void testCreateAirport_InvalidData() throws Exception {
        AirportDto invalidAirport = new AirportDto();
        // Missing required fields

        mockMvc.perform(post("/v1/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidAirport)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateAirport_NotImplemented() throws Exception {
        AirportDto newAirport = createSampleAirport();
        newAirport.setId(null);
        
        when(airportService.save(any(AirportDto.class))).thenThrow(new UnsupportedOperationException());

        mockMvc.perform(post("/v1/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAirport)))
                .andExpect(status().isNotImplemented());
    }

    // PUT /{id} (update) tests
    @Test
    void testUpdateAirport_Success() throws Exception {
        UUID id = sampleAirport.getId();
        AirportDto updatedAirport = createSampleAirport();
        updatedAirport.setAirportName("Updated Airport Name");
        
        when(airportService.update(eq(id), any(AirportDto.class))).thenReturn(updatedAirport);

        mockMvc.perform(put("/v1/airports/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleAirport)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.airportName").value("Updated Airport Name"));
    }

    @Test
    void testUpdateAirport_NotImplemented() throws Exception {
        UUID id = sampleAirport.getId();
        
        when(airportService.update(eq(id), any(AirportDto.class))).thenThrow(new UnsupportedOperationException());

        mockMvc.perform(put("/v1/airports/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleAirport)))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void testUpdateAirport_InvalidId() throws Exception {
        mockMvc.perform(put("/v1/airports/{id}", "invalid-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleAirport)))
                .andExpect(status().isBadRequest());
    }

    // DELETE /{id} (deactivate) tests
    @Test
    void testDeactivateAirport_Success() throws Exception {
        UUID id = sampleAirport.getId();
        
        // Successful deactivation returns void
        mockMvc.perform(delete("/v1/airports/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeactivateAirport_NotImplemented() throws Exception {
        UUID id = sampleAirport.getId();
        
        doThrow(new UnsupportedOperationException()).when(airportService).deactivate(id);

        mockMvc.perform(delete("/v1/airports/{id}", id))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void testDeactivateAirport_InvalidId() throws Exception {
        mockMvc.perform(delete("/v1/airports/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    // Temporal query tests (asOf parameter)
    @Test
    void testGetAirportByCode_WithAsOfDate() throws Exception {
        LocalDate asOfDate = LocalDate.of(2024, 6, 15);
        when(airportService.findByCodeAndSystemAsOf(eq("LAX"), eq("IATA"), eq(asOfDate)))
                .thenReturn(Optional.of(sampleAirport));

        // Note: This might conflict with the /{id} path, depending on route order
        mockMvc.perform(get("/v1/airports/{code}", "LAX")
                .param("codeSystem", "IATA")
                .param("asOf", "2024-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("LAX"));
    }

    @Test
    void testGetAirportByCode_WithoutAsOfDate() throws Exception {
        when(airportService.findByCodeAndSystem(eq("LAX"), eq("IATA")))
                .thenReturn(Optional.of(sampleAirport));

        mockMvc.perform(get("/v1/airports/{code}", "LAX")
                .param("codeSystem", "IATA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("LAX"));
    }

    @Test
    void testGetAirportByCode_InvalidDate() throws Exception {
        mockMvc.perform(get("/v1/airports/{code}", "LAX")
                .param("codeSystem", "IATA")
                .param("asOf", "invalid-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAirportByCode_InvalidCodeLength() throws Exception {
        mockMvc.perform(get("/v1/airports/{code}", "LA")
                .param("codeSystem", "IATA"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/v1/airports/{code}", "TOOLONG")
                .param("codeSystem", "IATA"))
                .andExpect(status().isBadRequest());
    }

    // Edge cases and error scenarios
    @Test
    void testGetAirportsBySystemCode_LargePageSize() throws Exception {
        List<AirportDto> airports = Arrays.asList(sampleAirport);
        PagedResponse<AirportDto> response = new PagedResponse<>(airports, 0, 1000, 1);
        
        when(airportService.findBySystemCode(eq("IATA"), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/v1/airports")
                .param("codeSystem", "IATA")
                .param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1000));
    }

    @Test
    void testSearchAirports_SpecialCharacters() throws Exception {
        List<AirportDto> airports = Arrays.asList(sampleAirport);
        PagedResponse<AirportDto> response = new PagedResponse<>(airports, 0, 20, 1);
        
        when(airportService.searchByNameCityCountry(eq("Los Ángeles"), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/v1/airports/search")
                .param("query", "Los Ángeles"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAirportsByCountry_CaseInsensitive() throws Exception {
        List<AirportDto> airports = Arrays.asList(sampleAirport);
        when(airportService.findByCountryCode("usa")).thenReturn(airports);

        mockMvc.perform(get("/v1/airports/by-country")
                .param("countryCode", "usa"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAirportsByCity_Unicode() throws Exception {
        AirportDto unicodeAirport = createSampleAirport();
        unicodeAirport.setCity("北京");
        
        when(airportService.findByCity("北京")).thenReturn(Arrays.asList(unicodeAirport));

        mockMvc.perform(get("/v1/airports/by-city")
                .param("city", "北京"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("北京"));
    }
}