package gov.dhs.cbp.reference.api.service;

import gov.dhs.cbp.reference.api.dto.search.SearchResult;
import gov.dhs.cbp.reference.api.dto.search.AutocompleteResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchService {
    
    /**
     * Universal search across multiple entity types
     */
    Page<SearchResult> universalSearch(String query, List<String> dataTypes, 
                                      Float similarityThreshold, Pageable pageable);
    
    /**
     * Search for countries with fuzzy matching
     */
    Page<SearchResult> searchCountries(String query, Float similarityThreshold, 
                                      Pageable pageable);
    
    /**
     * Search for ports with optional country filter
     */
    Page<SearchResult> searchPorts(String query, String countryCode, 
                                  Float similarityThreshold, Pageable pageable);
    
    /**
     * Search for airports with fuzzy matching
     */
    Page<SearchResult> searchAirports(String query, Float similarityThreshold, 
                                     Pageable pageable);
    
    /**
     * Autocomplete suggestions for quick lookup
     */
    List<AutocompleteResult> autocomplete(String prefix, String dataType, int limit);
}