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

    @Autowired
    private FF4j ff4j;

    @GetMapping("/feature-flags")
    public ResponseEntity<Map<String, Object>> getFeatureFlags() {
        Map<String, Object> flags = new HashMap<>();

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
            } else if (featureId.startsWith("features.")) {
                groupedFlags.computeIfAbsent("features", k -> new HashMap<>())
                    .put(featureId.substring(9), isEnabled);
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

        boolean enabled = (Boolean) request.get("enabled");

        // List of possible prefixes
        List<String> prefixes = Arrays.asList("features.", "experimental.", "dashboard.", "admin.");
        String fullFeatureId = featureId;

        // Check if the feature exists without a prefix (for referenceData)
        if (!ff4j.exist(fullFeatureId)) {
            // If not, try to find it with a prefix
            for (String prefix : prefixes) {
                if (ff4j.exist(prefix + featureId)) {
                    fullFeatureId = prefix + featureId;
                    break;
                }
            }
        }

        if (ff4j.exist(fullFeatureId)) {
            if (enabled) {
                ff4j.enable(fullFeatureId);
            } else {
                ff4j.disable(fullFeatureId);
            }

            Feature feature = ff4j.getFeature(fullFeatureId);
            return ResponseEntity.ok(Map.of(
                "featureId", fullFeatureId,
                "enabled", ff4j.check(fullFeatureId),
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


}