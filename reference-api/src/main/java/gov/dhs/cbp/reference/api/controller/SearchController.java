package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.dto.search.AutocompleteResult;
import gov.dhs.cbp.reference.api.dto.search.SearchResult;
import gov.dhs.cbp.reference.api.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/search")
@Tag(name = "Search", description = "Full-text and fuzzy search endpoints")
public class SearchController {
    
    private final SearchService searchService;
    
    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    
    @GetMapping
    @Operation(summary = "Universal search across all reference data")
    public ResponseEntity<Page<SearchResult>> universalSearch(
            @Parameter(description = "Search query", required = true)
            @RequestParam String q,
            
            @Parameter(description = "Data types to search (countries, ports, airports)")
            @RequestParam(required = false) List<String> types,
            
            @Parameter(description = "Similarity threshold for fuzzy matching (0.0-1.0)")
            @RequestParam(required = false, defaultValue = "0.3") Float threshold,
            
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        if (types == null || types.isEmpty()) {
            types = Arrays.asList("countries", "ports", "airports");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SearchResult> results = searchService.universalSearch(q, types, threshold, pageable);
        
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/countries")
    @Operation(summary = "Search for countries with fuzzy matching")
    public ResponseEntity<Page<SearchResult>> searchCountries(
            @Parameter(description = "Search query", required = true)
            @RequestParam String q,
            
            @Parameter(description = "Similarity threshold for fuzzy matching (0.0-1.0)")
            @RequestParam(required = false, defaultValue = "0.3") Float threshold,
            
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SearchResult> results = searchService.searchCountries(q, threshold, pageable);
        
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/autocomplete")
    @Operation(summary = "Get autocomplete suggestions")
    public ResponseEntity<List<AutocompleteResult>> autocomplete(
            @Parameter(description = "Search prefix", required = true)
            @RequestParam String prefix,
            
            @Parameter(description = "Data type (countries, ports, airports)")
            @RequestParam(required = false, defaultValue = "countries") String type,
            
            @Parameter(description = "Maximum number of suggestions")
            @RequestParam(required = false, defaultValue = "10") int limit) {
        
        List<AutocompleteResult> results = searchService.autocomplete(prefix, type, limit);
        
        return ResponseEntity.ok(results);
    }
}