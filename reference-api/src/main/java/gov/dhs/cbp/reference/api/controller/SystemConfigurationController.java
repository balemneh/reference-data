package gov.dhs.cbp.reference.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.dhs.cbp.reference.api.dto.SystemConfigurationDto;
import gov.dhs.cbp.reference.api.service.SystemConfigurationService;
import gov.dhs.cbp.reference.core.entity.SystemConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for system configuration management.
 */
@RestController
@RequestMapping("/api/v1/system-config")
@Tag(name = "System Configuration", description = "System configuration management endpoints")
@CrossOrigin(origins = "*")
public class SystemConfigurationController {

    private final SystemConfigurationService service;
    private final ObjectMapper objectMapper;

    @Autowired
    public SystemConfigurationController(SystemConfigurationService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all system configurations.
     */
    @GetMapping
    @Operation(summary = "Get all system configurations")
    public ResponseEntity<Map<String, SystemConfigurationDto>> getAllConfigurations() {
        Map<String, SystemConfigurationDto> configs = service.getAllConfigurations();
        return ResponseEntity.ok(configs);
    }

    /**
     * Get configuration by key.
     */
    @GetMapping("/{configKey}")
    @Operation(summary = "Get configuration by key")
    public ResponseEntity<SystemConfigurationDto> getConfiguration(@PathVariable String configKey) {
        SystemConfigurationDto config = service.getConfiguration(configKey);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    /**
     * Update configuration value.
     */
    @PutMapping("/{configKey}")
    @Operation(summary = "Update configuration value")
    public ResponseEntity<SystemConfigurationDto> updateConfiguration(
            @PathVariable String configKey,
            @RequestBody JsonNode configValue) {

        // Determine config type based on key
        SystemConfiguration.ConfigurationType type = determineConfigType(configKey);

        SystemConfigurationDto updated = service.saveConfiguration(configKey, configValue, type);
        return ResponseEntity.ok(updated);
    }

    /**
     * Update specific field in configuration.
     */
    @PatchMapping("/{configKey}/{fieldPath}")
    @Operation(summary = "Update specific field in configuration")
    public ResponseEntity<SystemConfigurationDto> updateConfigurationField(
            @PathVariable String configKey,
            @PathVariable String fieldPath,
            @RequestBody JsonNode value) {

        try {
            SystemConfigurationDto updated = service.updateConfigurationField(configKey, fieldPath, value);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Deactivate configuration.
     */
    @DeleteMapping("/{configKey}")
    @Operation(summary = "Deactivate configuration")
    public ResponseEntity<Void> deactivateConfiguration(@PathVariable String configKey) {
        try {
            service.deactivateConfiguration(configKey);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Feature flags endpoints moved to FeatureFlagsController which uses FF4J

    /**
     * Helper method to determine config type from key.
     */
    private SystemConfiguration.ConfigurationType determineConfigType(String configKey) {
        return SystemConfiguration.ConfigurationType.GENERAL;
    }

    /**
     * Create default feature flags configuration.
     */
    private JsonNode createDefaultFeatureFlags() {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode dataManagement = objectMapper.createObjectNode();
        dataManagement.put("bulkImport", true);
        dataManagement.put("exportToExcel", true);
        dataManagement.put("dataValidation", true);
        dataManagement.put("autoBackup", false);
        root.set("dataManagement", dataManagement);

        ObjectNode userInterface = objectMapper.createObjectNode();
        userInterface.put("darkMode", false);
        userInterface.put("advancedSearch", true);
        userInterface.put("customDashboards", false);
        userInterface.put("multiLanguage", false);
        root.set("userInterface", userInterface);

        ObjectNode integration = objectMapper.createObjectNode();
        integration.put("webhooks", true);
        integration.put("apiV2", false);
        integration.put("graphQL", false);
        integration.put("realtimeSync", true);
        root.set("integration", integration);

        ObjectNode security = objectMapper.createObjectNode();
        security.put("twoFactor", true);
        security.put("ssoIntegration", false);
        security.put("apiRateLimiting", true);
        security.put("encryptionAtRest", true);
        root.set("security", security);

        ObjectNode performance = objectMapper.createObjectNode();
        performance.put("caching", true);
        performance.put("lazyLoading", true);
        performance.put("compressionEnabled", false);
        performance.put("cdnIntegration", false);
        root.set("performance", performance);

        ObjectNode experimental = objectMapper.createObjectNode();
        experimental.put("betaFeatures", false);
        experimental.put("a_bTesting", false);
        experimental.put("debugMode", false);
        experimental.put("telemetry", true);
        root.set("experimental", experimental);

        return root;
    }
}