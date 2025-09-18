package gov.dhs.cbp.reference.api.controller;

import org.ff4j.FF4j;
import org.ff4j.core.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/system-config")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class FeatureFlagsController {

    @Autowired(required = false)
    private FF4j ff4j;

    @GetMapping("/feature-flags")
    public ResponseEntity<Map<String, Object>> getFeatureFlags() {
        Map<String, Object> flags = new HashMap<>();

        if (ff4j == null) {
            // Return default flags if FF4J is not available
            return ResponseEntity.ok(getDefaultFlags());
        }

        // Group features by category
        Map<String, Map<String, Boolean>> groupedFlags = new HashMap<>();

        Map<String, Feature> allFeatures = ff4j.getFeatures();
        for (Feature feature : allFeatures.values()) {
            String featureId = feature.getUid();
            boolean isEnabled = ff4j.check(featureId);

            // Parse feature ID to determine category
            if (featureId.startsWith("dashboard.")) {
                groupedFlags.computeIfAbsent("dashboard", k -> new HashMap<>())
                    .put(featureId.substring(10), isEnabled);
            } else if (featureId.startsWith("experimental.")) {
                groupedFlags.computeIfAbsent("experimental", k -> new HashMap<>())
                    .put(featureId.substring(13), isEnabled);
            } else {
                groupedFlags.computeIfAbsent("referenceData", k -> new HashMap<>())
                    .put(featureId, isEnabled);
            }
        }

        flags.putAll(groupedFlags);
        return ResponseEntity.ok(flags);
    }

    @PutMapping("/feature-flags/{featureId}")
    public ResponseEntity<Map<String, Object>> updateFeatureFlag(
            @PathVariable String featureId,
            @RequestBody Map<String, Object> request) {

        if (ff4j == null) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Feature flags service not available"
            ));
        }

        boolean enabled = (Boolean) request.get("enabled");

        if (ff4j.exist(featureId)) {
            if (enabled) {
                ff4j.enable(featureId);
            } else {
                ff4j.disable(featureId);
            }

            Feature feature = ff4j.getFeature(featureId);
            return ResponseEntity.ok(Map.of(
                "featureId", featureId,
                "enabled", ff4j.check(featureId),
                "description", feature.getDescription()
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/feature-flags")
    public ResponseEntity<Map<String, Object>> createFeatureFlag(@RequestBody Map<String, Object> request) {
        if (ff4j == null) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Feature flags service not available"
            ));
        }

        String featureId = (String) request.get("featureId");
        String description = (String) request.get("description");
        boolean enabled = request.containsKey("enabled") ? (Boolean) request.get("enabled") : false;

        if (ff4j.exist(featureId)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Feature flag already exists"
            ));
        }

        Feature feature = new Feature(featureId, enabled, description);
        ff4j.createFeature(feature);

        return ResponseEntity.ok(Map.of(
            "featureId", featureId,
            "enabled", enabled,
            "description", description
        ));
    }

    @DeleteMapping("/feature-flags/{featureId}")
    public ResponseEntity<Void> deleteFeatureFlag(@PathVariable String featureId) {
        if (ff4j == null) {
            return ResponseEntity.status(503).build();
        }

        if (ff4j.exist(featureId)) {
            ff4j.delete(featureId);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/feature-flags/{featureId}")
    public ResponseEntity<Map<String, Object>> getFeatureFlag(@PathVariable String featureId) {
        if (ff4j == null) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Feature flags service not available"
            ));
        }

        if (ff4j.exist(featureId)) {
            Feature feature = ff4j.getFeature(featureId);
            return ResponseEntity.ok(Map.of(
                "featureId", featureId,
                "enabled", ff4j.check(featureId),
                "description", feature.getDescription()
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private Map<String, Object> getDefaultFlags() {
        Map<String, Object> defaultFlags = new HashMap<>();

        // Dashboard-specific features (non-data related)
        Map<String, Boolean> dashboard = new HashMap<>();
        dashboard.put("showRecentActivity", true);
        dashboard.put("showSystemHealth", true);
        defaultFlags.put("dashboard", dashboard);

        // Reference data features (controls both data access AND dashboard visibility)
        Map<String, Boolean> referenceData = new HashMap<>();
        referenceData.put("countries", true);
        referenceData.put("ports", true);
        referenceData.put("airports", true);
        referenceData.put("carriers", true);
        referenceData.put("units", true);
        referenceData.put("languages", true);
        referenceData.put("changeRequests", true);
        referenceData.put("analytics", true);
        referenceData.put("export", true);
        referenceData.put("import", true);
        referenceData.put("bulkOperations", true);
        referenceData.put("advancedSearch", true);
        referenceData.put("apiAccess", true);
        referenceData.put("webhooks", false);
        defaultFlags.put("referenceData", referenceData);

        // Experimental features
        Map<String, Boolean> experimental = new HashMap<>();
        experimental.put("aiAssistant", false);
        experimental.put("graphView", false);
        experimental.put("realtimeSync", false);
        experimental.put("collaboration", false);
        defaultFlags.put("experimental", experimental);

        return defaultFlags;
    }
}