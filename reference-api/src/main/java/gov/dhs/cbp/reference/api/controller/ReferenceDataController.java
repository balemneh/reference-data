package gov.dhs.cbp.reference.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/reference-data")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ReferenceDataController {

    @GetMapping("/types")
    public ResponseEntity<List<Map<String, Object>>> getReferenceDataTypes() {
        List<Map<String, Object>> types = new ArrayList<>();

        // Countries type
        Map<String, Object> countriesType = new HashMap<>();
        countriesType.put("id", "countries");
        countriesType.put("name", "countries");
        countriesType.put("displayName", "Countries");
        countriesType.put("description", "Country reference data");
        countriesType.put("icon", "public");
        countriesType.put("category", "Geographic");
        countriesType.put("isEnabled", true);

        Map<String, Boolean> countriesPermissions = new HashMap<>();
        countriesPermissions.put("read", true);
        countriesPermissions.put("create", false);
        countriesPermissions.put("update", false);
        countriesPermissions.put("delete", false);
        countriesType.put("permissions", countriesPermissions);

        List<String> countriesSystemCodes = new ArrayList<>();
        countriesSystemCodes.add("ISO3166-1");
        countriesType.put("systemCodes", countriesSystemCodes);

        List<Map<String, Object>> countriesFields = new ArrayList<>();

        Map<String, Object> countryCodeField = new HashMap<>();
        countryCodeField.put("name", "countryCode");
        countryCodeField.put("displayName", "Country Code");
        countryCodeField.put("type", "string");
        countryCodeField.put("required", true);
        countryCodeField.put("maxLength", 5);
        countryCodeField.put("searchable", true);
        countryCodeField.put("sortable", true);
        countryCodeField.put("showInList", true);
        countriesFields.add(countryCodeField);

        Map<String, Object> countryNameField = new HashMap<>();
        countryNameField.put("name", "countryName");
        countryNameField.put("displayName", "Country Name");
        countryNameField.put("type", "string");
        countryNameField.put("required", true);
        countryNameField.put("maxLength", 100);
        countryNameField.put("searchable", true);
        countryNameField.put("sortable", true);
        countryNameField.put("showInList", true);
        countriesFields.add(countryNameField);

        countriesType.put("fields", countriesFields);
        types.add(countriesType);

        // Airports type
        Map<String, Object> airportsType = new HashMap<>();
        airportsType.put("id", "airports");
        airportsType.put("name", "airports");
        airportsType.put("displayName", "Airports");
        airportsType.put("description", "Airport reference data");
        airportsType.put("icon", "flight");
        airportsType.put("category", "Transportation");
        airportsType.put("isEnabled", true);

        Map<String, Boolean> airportsPermissions = new HashMap<>();
        airportsPermissions.put("read", true);
        airportsPermissions.put("create", false);
        airportsPermissions.put("update", false);
        airportsPermissions.put("delete", false);
        airportsType.put("permissions", airportsPermissions);

        List<String> airportsSystemCodes = new ArrayList<>();
        airportsSystemCodes.add("IATA");
        airportsSystemCodes.add("ICAO");
        airportsType.put("systemCodes", airportsSystemCodes);

        List<Map<String, Object>> airportsFields = new ArrayList<>();

        Map<String, Object> airportCodeField = new HashMap<>();
        airportCodeField.put("name", "airportCode");
        airportCodeField.put("displayName", "Airport Code");
        airportCodeField.put("type", "string");
        airportCodeField.put("required", true);
        airportCodeField.put("maxLength", 10);
        airportCodeField.put("searchable", true);
        airportCodeField.put("sortable", true);
        airportCodeField.put("showInList", true);
        airportsFields.add(airportCodeField);

        Map<String, Object> airportNameField = new HashMap<>();
        airportNameField.put("name", "airportName");
        airportNameField.put("displayName", "Airport Name");
        airportNameField.put("type", "string");
        airportNameField.put("required", true);
        airportNameField.put("maxLength", 100);
        airportNameField.put("searchable", true);
        airportNameField.put("sortable", true);
        airportNameField.put("showInList", true);
        airportsFields.add(airportNameField);

        airportsType.put("fields", airportsFields);
        types.add(airportsType);

        // Ports type
        Map<String, Object> portsType = new HashMap<>();
        portsType.put("id", "ports");
        portsType.put("name", "ports");
        portsType.put("displayName", "Ports");
        portsType.put("description", "Port reference data");
        portsType.put("icon", "directions_boat");
        portsType.put("category", "Transportation");
        portsType.put("isEnabled", true);

        Map<String, Boolean> portsPermissions = new HashMap<>();
        portsPermissions.put("read", true);
        portsPermissions.put("create", false);
        portsPermissions.put("update", false);
        portsPermissions.put("delete", false);
        portsType.put("permissions", portsPermissions);

        List<String> portsSystemCodes = new ArrayList<>();
        portsSystemCodes.add("UNLOCODE");
        portsType.put("systemCodes", portsSystemCodes);

        List<Map<String, Object>> portsFields = new ArrayList<>();

        Map<String, Object> portCodeField = new HashMap<>();
        portCodeField.put("name", "portCode");
        portCodeField.put("displayName", "Port Code");
        portCodeField.put("type", "string");
        portCodeField.put("required", true);
        portCodeField.put("maxLength", 10);
        portCodeField.put("searchable", true);
        portCodeField.put("sortable", true);
        portCodeField.put("showInList", true);
        portsFields.add(portCodeField);

        Map<String, Object> portNameField = new HashMap<>();
        portNameField.put("name", "portName");
        portNameField.put("displayName", "Port Name");
        portNameField.put("type", "string");
        portNameField.put("required", true);
        portNameField.put("maxLength", 100);
        portNameField.put("searchable", true);
        portNameField.put("sortable", true);
        portNameField.put("showInList", true);
        portsFields.add(portNameField);

        portsType.put("fields", portsFields);
        types.add(portsType);

        // Organizations type
        Map<String, Object> organizationsType = new HashMap<>();
        organizationsType.put("id", "organizations");
        organizationsType.put("name", "organizations");
        organizationsType.put("displayName", "Organizations");
        organizationsType.put("description", "Organization reference data");
        organizationsType.put("icon", "business");
        organizationsType.put("category", "Entity");
        organizationsType.put("isEnabled", false);

        Map<String, Boolean> organizationsPermissions = new HashMap<>();
        organizationsPermissions.put("read", true);
        organizationsPermissions.put("create", false);
        organizationsPermissions.put("update", false);
        organizationsPermissions.put("delete", false);
        organizationsType.put("permissions", organizationsPermissions);

        List<String> organizationsSystemCodes = new ArrayList<>();
        organizationsSystemCodes.add("CBP-ORG");
        organizationsType.put("systemCodes", organizationsSystemCodes);

        List<Map<String, Object>> organizationsFields = new ArrayList<>();
        organizationsType.put("fields", organizationsFields);
        types.add(organizationsType);

        // Locations type
        Map<String, Object> locationsType = new HashMap<>();
        locationsType.put("id", "locations");
        locationsType.put("name", "locations");
        locationsType.put("displayName", "Locations");
        locationsType.put("description", "Location reference data");
        locationsType.put("icon", "location_on");
        locationsType.put("category", "Geographic");
        locationsType.put("isEnabled", false);

        Map<String, Boolean> locationsPermissions = new HashMap<>();
        locationsPermissions.put("read", true);
        locationsPermissions.put("create", false);
        locationsPermissions.put("update", false);
        locationsPermissions.put("delete", false);
        locationsType.put("permissions", locationsPermissions);

        List<String> locationsSystemCodes = new ArrayList<>();
        locationsSystemCodes.add("CBP-LOC");
        locationsType.put("systemCodes", locationsSystemCodes);

        List<Map<String, Object>> locationsFields = new ArrayList<>();
        locationsType.put("fields", locationsFields);
        types.add(locationsType);

        // Products type
        Map<String, Object> productsType = new HashMap<>();
        productsType.put("id", "products");
        productsType.put("name", "products");
        productsType.put("displayName", "Products");
        productsType.put("description", "Product reference data");
        productsType.put("icon", "inventory_2");
        productsType.put("category", "Trade");
        productsType.put("isEnabled", false);

        Map<String, Boolean> productsPermissions = new HashMap<>();
        productsPermissions.put("read", true);
        productsPermissions.put("create", false);
        productsPermissions.put("update", false);
        productsPermissions.put("delete", false);
        productsType.put("permissions", productsPermissions);

        List<String> productsSystemCodes = new ArrayList<>();
        productsSystemCodes.add("HTS");
        productsType.put("systemCodes", productsSystemCodes);

        List<Map<String, Object>> productsFields = new ArrayList<>();
        productsType.put("fields", productsFields);
        types.add(productsType);

        return ResponseEntity.ok(types);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Reference data types stats
        List<Map<String, Object>> referenceDataTypes = new ArrayList<>();

        Map<String, Object> countriesStats = new HashMap<>();
        countriesStats.put("type", "countries");
        countriesStats.put("total", 50);
        countriesStats.put("active", 50);
        countriesStats.put("lastUpdated", "2025-09-17T00:00:00Z");
        referenceDataTypes.add(countriesStats);

        Map<String, Object> airportsStats = new HashMap<>();
        airportsStats.put("type", "airports");
        airportsStats.put("total", 25);
        airportsStats.put("active", 25);
        airportsStats.put("lastUpdated", "2025-09-17T00:00:00Z");
        referenceDataTypes.add(airportsStats);

        Map<String, Object> portsStats = new HashMap<>();
        portsStats.put("type", "ports");
        portsStats.put("total", 25);
        portsStats.put("active", 25);
        portsStats.put("lastUpdated", "2025-09-17T00:00:00Z");
        referenceDataTypes.add(portsStats);

        stats.put("referenceDataTypes", referenceDataTypes);

        // Change requests stats
        Map<String, Object> changeRequests = new HashMap<>();
        changeRequests.put("pending", 3);
        changeRequests.put("approved", 12);
        changeRequests.put("rejected", 2);
        stats.put("changeRequests", changeRequests);

        // System health
        Map<String, Object> systemHealth = new HashMap<>();
        systemHealth.put("status", "UP");

        Map<String, Object> components = new HashMap<>();
        Map<String, Object> database = new HashMap<>();
        database.put("status", "UP");
        components.put("database", database);

        Map<String, Object> redis = new HashMap<>();
        redis.put("status", "UP");
        components.put("redis", redis);

        Map<String, Object> kafka = new HashMap<>();
        kafka.put("status", "UP");
        components.put("kafka", kafka);

        Map<String, Object> api = new HashMap<>();
        api.put("status", "UP");
        components.put("api", api);

        systemHealth.put("components", components);
        stats.put("systemHealth", systemHealth);

        // Recent activity
        List<Map<String, Object>> recentActivity = new ArrayList<>();

        Map<String, Object> activity1 = new HashMap<>();
        activity1.put("type", "CREATE");
        activity1.put("entityType", "Country");
        activity1.put("entityName", "New Country");
        activity1.put("user", "admin");
        activity1.put("timestamp", "2025-09-17T08:00:00Z");
        recentActivity.add(activity1);

        Map<String, Object> activity2 = new HashMap<>();
        activity2.put("type", "UPDATE");
        activity2.put("entityType", "Airport");
        activity2.put("entityName", "JFK International");
        activity2.put("user", "user1");
        activity2.put("timestamp", "2025-09-17T07:30:00Z");
        recentActivity.add(activity2);

        stats.put("recentActivity", recentActivity);

        return ResponseEntity.ok(stats);
    }
}