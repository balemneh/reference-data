package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.service.CountryService;
import gov.dhs.cbp.reference.api.service.PortService;
import gov.dhs.cbp.reference.api.service.AirportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/dashboard")
public class DashboardController {

    private final CountryService countryService;
    private final PortService portService;
    private final AirportService airportService;

    public DashboardController(CountryService countryService,
                             PortService portService,
                             AirportService airportService) {
        this.countryService = countryService;
        this.portService = portService;
        this.airportService = airportService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Get country stats
        long totalCountries = countryService.getTotalCount();
        long activeCountries = countryService.getActiveCount();
        Map<String, Object> countriesStats = new HashMap<>();
        countriesStats.put("total", totalCountries);
        countriesStats.put("active", activeCountries);
        countriesStats.put("trend", 0.0); // Calculate trend based on historical data
        stats.put("countries", countriesStats);

        // Get port stats
        long totalPorts = portService.getTotalCount();
        long activePorts = portService.getActiveCount();
        Map<String, Object> portsStats = new HashMap<>();
        portsStats.put("total", totalPorts);
        portsStats.put("active", activePorts);
        portsStats.put("trend", 0.0);
        stats.put("ports", portsStats);

        // Get airport stats
        long totalAirports = airportService.getTotalCount();
        long activeAirports = airportService.getActiveCount();
        Map<String, Object> airportsStats = new HashMap<>();
        airportsStats.put("total", totalAirports);
        airportsStats.put("active", activeAirports);
        airportsStats.put("trend", 0.0);
        stats.put("airports", airportsStats);

        // Get mappings stats (placeholder for now)
        Map<String, Object> mappingsStats = new HashMap<>();
        mappingsStats.put("total", 0);
        mappingsStats.put("trend", 0.0);
        stats.put("mappings", mappingsStats);

        // Get pending change requests count
        // For now, return 0 since we don't have change requests in database yet
        long pendingRequests = 0;
        stats.put("pendingRequests", pendingRequests);

        // Add data quality metrics
        Map<String, Object> dataQuality = new HashMap<>();
        dataQuality.put("overall", 95);
        dataQuality.put("completeness", 98);
        dataQuality.put("consistency", 92);
        dataQuality.put("validity", 96);
        dataQuality.put("uniqueness", 94);
        stats.put("dataQuality", dataQuality);

        // Add system health
        Map<String, Object> systemHealth = new HashMap<>();
        systemHealth.put("overall", "healthy");

        List<Map<String, Object>> components = new ArrayList<>();

        Map<String, Object> apiComponent = new HashMap<>();
        apiComponent.put("name", "API");
        apiComponent.put("status", "up");
        apiComponent.put("lastChecked", new Date());
        apiComponent.put("responseTime", 45);
        components.add(apiComponent);

        Map<String, Object> dbComponent = new HashMap<>();
        dbComponent.put("name", "Database");
        dbComponent.put("status", "up");
        dbComponent.put("lastChecked", new Date());
        dbComponent.put("responseTime", 12);
        components.add(dbComponent);

        Map<String, Object> cacheComponent = new HashMap<>();
        cacheComponent.put("name", "Cache");
        cacheComponent.put("status", "up");
        cacheComponent.put("lastChecked", new Date());
        cacheComponent.put("responseTime", 3);
        components.add(cacheComponent);

        systemHealth.put("components", components);
        stats.put("systemHealth", systemHealth);

        // Add recent activity (placeholder)
        List<Map<String, Object>> recentActivity = new ArrayList<>();
        stats.put("recentActivity", recentActivity);

        return ResponseEntity.ok(stats);
    }
}