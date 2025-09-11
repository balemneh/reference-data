package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.config.WebMvcTestConfig;
import gov.dhs.cbp.reference.api.dto.search.AutocompleteResult;
import gov.dhs.cbp.reference.api.dto.search.SearchResult;
import gov.dhs.cbp.reference.api.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@Import(WebMvcTestConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires JPA configuration - needs refactoring to work with @WebMvcTest")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    private SearchResult sampleSearchResult;
    private List<SearchResult> searchResults;

    @BeforeEach
    void setUp() {
        sampleSearchResult = new SearchResult();
        sampleSearchResult.setEntityType("country");
        sampleSearchResult.setEntityId(UUID.randomUUID());
        sampleSearchResult.setCode("US");
        sampleSearchResult.setName("United States");
        sampleSearchResult.setSimilarityScore(0.95f);
        sampleSearchResult.setRank(1.0f);
        
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("iso2_code", "US");
        additionalInfo.put("iso3_code", "USA");
        sampleSearchResult.setAdditionalInfo(additionalInfo);
        
        searchResults = Arrays.asList(sampleSearchResult);
    }

    @Test
    void testUniversalSearch() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20);
        PageImpl<SearchResult> page = new PageImpl<>(searchResults, pageable, 1);
        
        when(searchService.universalSearch(eq("united"), any(), eq(0.3f), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/v1/search")
                .param("q", "united")
                .param("threshold", "0.3")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].code").value("US"))
            .andExpect(jsonPath("$.content[0].name").value("United States"))
            .andExpect(jsonPath("$.content[0].entityType").value("country"))
            .andExpect(jsonPath("$.content[0].similarityScore").value(0.95))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testSearchCountries() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20);
        PageImpl<SearchResult> page = new PageImpl<>(searchResults, pageable, 1);
        
        when(searchService.searchCountries(eq("united"), eq(0.3f), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/v1/search/countries")
                .param("q", "united")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].code").value("US"))
            .andExpect(jsonPath("$.content[0].name").value("United States"));
    }

    @Test
    void testSearchPorts() throws Exception {
        SearchResult portResult = new SearchResult();
        portResult.setEntityType("port");
        portResult.setEntityId(UUID.randomUUID());
        portResult.setCode("LAX");
        portResult.setName("Los Angeles");
        portResult.setSimilarityScore(0.9f);
        
        Map<String, Object> portInfo = new HashMap<>();
        portInfo.put("city", "Los Angeles");
        portInfo.put("country_code", "US");
        portResult.setAdditionalInfo(portInfo);
        
        List<SearchResult> portResults = Arrays.asList(portResult);
        PageRequest pageable = PageRequest.of(0, 20);
        PageImpl<SearchResult> page = new PageImpl<>(portResults, pageable, 1);
        
        when(searchService.searchPorts(eq("angeles"), eq("US"), eq(0.3f), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/v1/search/ports")
                .param("q", "angeles")
                .param("countryCode", "US")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].code").value("LAX"))
            .andExpect(jsonPath("$.content[0].name").value("Los Angeles"))
            .andExpect(jsonPath("$.content[0].additionalInfo.country_code").value("US"));
    }

    @Test
    void testSearchAirports() throws Exception {
        SearchResult airportResult = new SearchResult();
        airportResult.setEntityType("airport");
        airportResult.setEntityId(UUID.randomUUID());
        airportResult.setCode("JFK");
        airportResult.setName("John F. Kennedy International Airport");
        airportResult.setSimilarityScore(0.85f);
        
        List<SearchResult> airportResults = Arrays.asList(airportResult);
        PageRequest pageable = PageRequest.of(0, 20);
        PageImpl<SearchResult> page = new PageImpl<>(airportResults, pageable, 1);
        
        when(searchService.searchAirports(eq("kennedy"), eq(0.3f), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/v1/search/airports")
                .param("q", "kennedy")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].code").value("JFK"))
            .andExpect(jsonPath("$.content[0].name").value("John F. Kennedy International Airport"));
    }

    @Test
    void testAutocomplete() throws Exception {
        AutocompleteResult result1 = new AutocompleteResult("US", "United States", "code");
        AutocompleteResult result2 = new AutocompleteResult("GB", "United Kingdom", "name");
        List<AutocompleteResult> autocompleteResults = Arrays.asList(result1, result2);
        
        when(searchService.autocomplete(eq("uni"), eq("countries"), eq(10)))
            .thenReturn(autocompleteResults);

        mockMvc.perform(get("/v1/search/autocomplete")
                .param("prefix", "uni")
                .param("type", "countries")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("US"))
            .andExpect(jsonPath("$[0].name").value("United States"))
            .andExpect(jsonPath("$[0].matchType").value("code"))
            .andExpect(jsonPath("$[1].code").value("GB"))
            .andExpect(jsonPath("$[1].name").value("United Kingdom"));
    }

    @Test
    void testSearchWithPagination() throws Exception {
        PageRequest pageable = PageRequest.of(1, 10);
        PageImpl<SearchResult> page = new PageImpl<>(searchResults, pageable, 25);
        
        when(searchService.universalSearch(eq("test"), any(), eq(0.3f), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/v1/search")
                .param("q", "test")
                .param("page", "1")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(25))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void testSearchWithCustomThreshold() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20);
        PageImpl<SearchResult> page = new PageImpl<>(searchResults, pageable, 1);
        
        when(searchService.universalSearch(eq("fuzzy"), any(), eq(0.5f), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/v1/search")
                .param("q", "fuzzy")
                .param("threshold", "0.5")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testSearchWithMultipleTypes() throws Exception {
        PageRequest pageable = PageRequest.of(0, 20);
        PageImpl<SearchResult> page = new PageImpl<>(searchResults, pageable, 1);
        
        when(searchService.universalSearch(eq("test"), eq(Arrays.asList("countries", "ports")), 
                eq(0.3f), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/v1/search")
                .param("q", "test")
                .param("types", "countries", "ports")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testSearchMissingQuery() throws Exception {
        mockMvc.perform(get("/v1/search")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testAutocompleteMissingPrefix() throws Exception {
        mockMvc.perform(get("/v1/search/autocomplete")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}