package gov.dhs.cbp.reference.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.config.WebMvcTestConfig;
import gov.dhs.cbp.reference.api.dto.*;
import gov.dhs.cbp.reference.api.mapper.ChangeRequestMapper;
import gov.dhs.cbp.reference.api.mapper.CountryMapper;
import gov.dhs.cbp.reference.api.service.CountryChangeRequestService;
import gov.dhs.cbp.reference.api.service.CountryService;
import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = CountriesController.class)
@ContextConfiguration(classes = WebMvcTestConfig.class)
class CountriesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CountryService countryService;

    @MockBean
    private CountryChangeRequestService countryChangeRequestService;

    @MockBean
    private ChangeRequestMapper changeRequestMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private CountryDto sampleCountry;
    private ChangeRequest testChangeRequest;
    private ChangeRequestDto testChangeRequestDto;

    @BeforeEach
    void setUp() {
        sampleCountry = new CountryDto();
        sampleCountry.setId(UUID.randomUUID());
        sampleCountry.setCountryName("United States");
        sampleCountry.setCountryCode("US");
        sampleCountry.setIso2Code("US");
        sampleCountry.setIso3Code("USA");
        sampleCountry.setNumericCode("840");
        sampleCountry.setCodeSystem("ISO3166-1");
        sampleCountry.setValidFrom(LocalDate.now());

        testChangeRequest = new ChangeRequest();
        testChangeRequest.setId(UUID.randomUUID());
        testChangeRequest.setCrNumber("CR-2025-123456");
        testChangeRequest.setStatus("PENDING");
        testChangeRequest.setOperationType("CREATE");
        testChangeRequest.setTitle("Create new country: United States");
        testChangeRequest.setDescription("Adding United States country record");
        testChangeRequest.setReason("Initial setup");
        testChangeRequest.setRequesterId("user123");

        testChangeRequestDto = new ChangeRequestDto();
        testChangeRequestDto.setId(testChangeRequest.getId());
        testChangeRequestDto.setCrNumber(testChangeRequest.getCrNumber());
        testChangeRequestDto.setStatus(testChangeRequest.getStatus());
        testChangeRequestDto.setOperationType(testChangeRequest.getOperationType());
        testChangeRequestDto.setTitle(testChangeRequest.getTitle());
        testChangeRequestDto.setSubmittedBy(testChangeRequest.getRequesterId());
    }

    @Test
    void testGetCountriesBySystemCode() throws Exception {
        List<CountryDto> countries = Arrays.asList(sampleCountry);
        PagedResponse<CountryDto> response = new PagedResponse<>(countries, 0, 20, 1);
        
        when(countryService.findBySystemCode(eq("ISO3166-1"), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/v1/countries")
                .param("systemCode", "ISO3166-1")
                .param("page", "0")
                .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].countryName").value("United States"))
                .andExpect(jsonPath("$.content[0].iso3Code").value("USA"));
    }

    @Test
    void testGetCountryById() throws Exception {
        UUID id = sampleCountry.getId();
        when(countryService.findById(id)).thenReturn(Optional.of(sampleCountry));

        mockMvc.perform(get("/v1/countries/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryName").value("United States"))
                .andExpect(jsonPath("$.iso3Code").value("USA"));
    }

    @Test
    void testGetCountryByIdNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(countryService.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/countries/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetCountryByCodeAndSystem() throws Exception {
        when(countryService.findByCodeAndSystem(eq("USA"), eq("ISO3166-1"))).thenReturn(Optional.of(sampleCountry));

        mockMvc.perform(get("/v1/countries/by-code")
                .param("code", "USA")
                .param("systemCode", "ISO3166-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryName").value("United States"))
                .andExpect(jsonPath("$.iso3Code").value("USA"));
    }

    @Test
    void testGetCountryByCodeAndSystemAsOf() throws Exception {
        LocalDate asOf = LocalDate.of(2024, 6, 15);
        when(countryService.findByCodeAndSystemAsOf(eq("USA"), eq("ISO3166-1"), eq(asOf))).thenReturn(Optional.of(sampleCountry));

        mockMvc.perform(get("/v1/countries/by-code")
                .param("code", "USA")
                .param("systemCode", "ISO3166-1")
                .param("asOf", "2024-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryName").value("United States"));
    }

    @Test
    void testSearchCountries() throws Exception {
        List<CountryDto> countries = Arrays.asList(sampleCountry);
        PagedResponse<CountryDto> response = new PagedResponse<>(countries, 0, 20, 1);
        
        when(countryService.searchByName(eq("United"), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/v1/countries/search")
                .param("name", "United"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].countryName").value("United States"));
    }

    @Test
    void testGetAllCurrentCountries() throws Exception {
        List<CountryDto> countries = Arrays.asList(sampleCountry);
        when(countryService.findAllCurrent()).thenReturn(countries);

        mockMvc.perform(get("/v1/countries/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].countryName").value("United States"));
    }

    @Test
    void testGetCountriesBySystemCode_WithCustomPageSize() throws Exception {
        List<CountryDto> countries = Arrays.asList(sampleCountry);
        PagedResponse<CountryDto> response = new PagedResponse<>(countries, 0, 5, 1);
        
        when(countryService.findBySystemCode(eq("ISO3166-1"), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/v1/countries")
                .param("systemCode", "ISO3166-1")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].countryName").value("United States"))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetCountriesBySystemCode_WithHighPageNumber() throws Exception {
        PagedResponse<CountryDto> emptyResponse = new PagedResponse<>(Collections.emptyList(), 10, 20, 0);
        
        when(countryService.findBySystemCode(eq("ISO3166-1"), any(PageRequest.class))).thenReturn(emptyResponse);

        mockMvc.perform(get("/v1/countries")
                .param("systemCode", "ISO3166-1")
                .param("page", "10")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(10))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testGetCountriesBySystemCode_MissingSystemCode_BadRequest() throws Exception {
        mockMvc.perform(get("/v1/countries")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetCountryByCodeAndSystem_NotFound() throws Exception {
        when(countryService.findByCodeAndSystem(eq("INVALID"), eq("ISO3166-1"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/countries/by-code")
                .param("code", "INVALID")
                .param("systemCode", "ISO3166-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetCountryByCodeAndSystemAsOf_NotFound() throws Exception {
        LocalDate asOf = LocalDate.of(2020, 1, 1);
        when(countryService.findByCodeAndSystemAsOf(eq("USA"), eq("ISO3166-1"), eq(asOf))).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/countries/by-code")
                .param("code", "USA")
                .param("systemCode", "ISO3166-1")
                .param("asOf", "2020-01-01"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetCountryByCodeAndSystemAsOf_InvalidDateFormat() throws Exception {
        mockMvc.perform(get("/v1/countries/by-code")
                .param("code", "USA")
                .param("systemCode", "ISO3166-1")
                .param("asOf", "invalid-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchCountries_WithCustomPageSize() throws Exception {
        List<CountryDto> countries = Arrays.asList(sampleCountry);
        PagedResponse<CountryDto> response = new PagedResponse<>(countries, 0, 5, 1);
        
        when(countryService.searchByName(eq("United"), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/v1/countries/search")
                .param("name", "United")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].countryName").value("United States"))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void testSearchCountries_EmptySearchTerm() throws Exception {
        PagedResponse<CountryDto> emptyResponse = new PagedResponse<>(Collections.emptyList(), 0, 20, 0);
        
        when(countryService.searchByName(eq(""), any(PageRequest.class))).thenReturn(emptyResponse);

        mockMvc.perform(get("/v1/countries/search")
                .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void testSearchCountries_MissingNameParameter_BadRequest() throws Exception {
        mockMvc.perform(get("/v1/countries/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllCurrentCountries_EmptyResult() throws Exception {
        when(countryService.findAllCurrent()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/countries/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testSearchCountries_LargePageSize() throws Exception {
        List<CountryDto> countries = Arrays.asList(sampleCountry);
        PagedResponse<CountryDto> response = new PagedResponse<>(countries, 0, 1000, 1);
        
        when(countryService.searchByName(eq("United"), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/v1/countries/search")
                .param("name", "United")
                .param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1000));
    }

    @Test
    void testGetCountryByCodeAndSystem_WithSpecialCharacters() throws Exception {
        when(countryService.findByCodeAndSystem(eq("USA"), eq("ISO3166-1"))).thenReturn(Optional.of(sampleCountry));

        mockMvc.perform(get("/v1/countries/by-code")
                .param("code", "USA")
                .param("systemCode", "ISO3166-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryName").value("United States"));
    }

    @Test
    void testGetCountriesBySystemCode_WithSpecialPageParameters() throws Exception {
        List<CountryDto> countries = Arrays.asList(sampleCountry);
        PagedResponse<CountryDto> response = new PagedResponse<>(countries, 1, 50, 51);
        
        when(countryService.findBySystemCode(eq("ISO3166-1"), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/v1/countries")
                .param("systemCode", "ISO3166-1")
                .param("page", "1")
                .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(51));
    }

    @Test
    void testGetCountryById_WithValidUuid() throws Exception {
        UUID validId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        CountryDto country = new CountryDto();
        country.setId(validId);
        country.setCountryName("Test Country");
        
        when(countryService.findById(validId)).thenReturn(Optional.of(country));

        mockMvc.perform(get("/v1/countries/{id}", validId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryName").value("Test Country"));
    }

    @Test
    void testGetCountryById_WithInvalidUuidFormat() throws Exception {
        mockMvc.perform(get("/v1/countries/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    // New tests for change request workflow
    @Test
    void testCreateCountry_ShouldCreateChangeRequest() throws Exception {
        ChangeRequestCreateDto createDto = new ChangeRequestCreateDto(sampleCountry, "Initial creation");

        when(countryChangeRequestService.createChangeRequest(
                any(CountryDto.class),
                eq("CREATE"),
                anyString(),
                anyString()
        )).thenReturn(testChangeRequest);

        when(changeRequestMapper.toDto(testChangeRequest)).thenReturn(testChangeRequestDto);

        mockMvc.perform(post("/v1/countries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(testChangeRequest.getId().toString()))
                .andExpect(jsonPath("$.crNumber").value("CR-2025-123456"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testUpdateCountry_ShouldCreateChangeRequest() throws Exception {
        UUID countryId = sampleCountry.getId();
        sampleCountry.setCountryName("United States of America");
        ChangeRequestCreateDto updateDto = new ChangeRequestCreateDto(sampleCountry, "Updating country name");

        testChangeRequest.setOperationType("UPDATE");
        when(countryChangeRequestService.createChangeRequest(
                any(CountryDto.class),
                eq("UPDATE"),
                anyString(),
                anyString()
        )).thenReturn(testChangeRequest);

        when(changeRequestMapper.toDto(testChangeRequest)).thenReturn(testChangeRequestDto);

        mockMvc.perform(put("/v1/countries/{id}", countryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(testChangeRequest.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testDeleteCountry_ShouldCreateChangeRequest() throws Exception {
        UUID countryId = sampleCountry.getId();
        when(countryService.findById(countryId)).thenReturn(Optional.of(sampleCountry));

        testChangeRequest.setOperationType("DELETE");
        when(countryChangeRequestService.createChangeRequest(
                any(CountryDto.class),
                eq("DELETE"),
                anyString(),
                anyString()
        )).thenReturn(testChangeRequest);

        when(changeRequestMapper.toDto(testChangeRequest)).thenReturn(testChangeRequestDto);

        mockMvc.perform(delete("/v1/countries/{id}", countryId)
                        .param("reason", "No longer needed"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(testChangeRequest.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testApproveChangeRequest_ShouldApproveSuccessfully() throws Exception {
        ApprovalRequestDto approvalRequest = new ApprovalRequestDto("approver123", "Looks good");

        testChangeRequest.setStatus("APPROVED");
        when(countryChangeRequestService.approveChangeRequest(
                eq(testChangeRequest.getId()),
                anyString(),
                anyString()
        )).thenReturn(testChangeRequest);
        when(changeRequestMapper.toDto(testChangeRequest)).thenReturn(testChangeRequestDto);

        mockMvc.perform(post("/v1/countries/change-requests/{id}/approve", testChangeRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testChangeRequest.getId().toString()));
    }

    @Test
    void testRejectChangeRequest_ShouldRejectSuccessfully() throws Exception {
        RejectionRequestDto rejectionRequest = new RejectionRequestDto("rejector123", "Invalid data");

        testChangeRequest.setStatus("REJECTED");
        when(countryChangeRequestService.rejectChangeRequest(
                eq(testChangeRequest.getId()),
                anyString(),
                anyString()
        )).thenReturn(testChangeRequest);
        when(changeRequestMapper.toDto(testChangeRequest)).thenReturn(testChangeRequestDto);

        mockMvc.perform(post("/v1/countries/change-requests/{id}/reject", testChangeRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testChangeRequest.getId().toString()));
    }
}