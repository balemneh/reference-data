package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.service.CountryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v1/datasets")
@Tag(name = "Datasets", description = "Dataset statistics and metadata operations")
public class DatasetsController {
    
    private final CountryService countryService;
    
    public DatasetsController(CountryService countryService) {
        this.countryService = countryService;
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get dataset statistics",
               description = "Retrieve counts and statistics for all reference data types")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<Map<String, Object>> getDatasetStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Country stats
        Map<String, Object> countryStats = new HashMap<>();
        countryStats.put("total", countryService.getTotalCount());
        countryStats.put("active", countryService.getActiveCount());
        stats.put("countries", countryStats);
        
        // Mapping stats
        Map<String, Object> mappingStats = new HashMap<>();
        mappingStats.put("total", 0); // TODO: Implement when mapping service is ready
        stats.put("mappings", mappingStats);
        
        // Change request stats - TODO: Implement when workflow integration is complete
        stats.put("pendingRequests", 0);
        
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                .body(stats);
    }
    
    @GetMapping("/metadata")
    @Operation(summary = "Get dataset metadata",
               description = "Retrieve metadata information for all datasets")
    @ApiResponse(responseCode = "200", description = "Metadata retrieved successfully")
    public ResponseEntity<Map<String, Object>> getDatasetMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("countries", Map.of(
            "description", "ISO 3166-1 country codes and related code systems",
            "lastUpdated", countryService.getLastUpdated(),
            "version", countryService.getCurrentVersion(),
            "sources", Map.of(
                "iso", "ISO 3166-1 standard",
                "genc", "GENC geopolitical entities"
            )
        ));
        
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(metadata);
    }
}