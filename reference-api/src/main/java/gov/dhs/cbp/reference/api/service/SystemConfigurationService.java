package gov.dhs.cbp.reference.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.dhs.cbp.reference.api.dto.SystemConfigurationDto;
import gov.dhs.cbp.reference.core.entity.SystemConfiguration;
import gov.dhs.cbp.reference.core.repository.SystemConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing system configuration.
 */
@Service
@Transactional
public class SystemConfigurationService {

    private final SystemConfigurationRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    public SystemConfigurationService(SystemConfigurationRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all active configurations.
     */
    @Cacheable("systemConfiguration")
    public Map<String, SystemConfigurationDto> getAllConfigurations() {
        List<SystemConfiguration> configs = repository.findByIsActiveTrue();
        Map<String, SystemConfigurationDto> result = new HashMap<>();

        for (SystemConfiguration config : configs) {
            SystemConfigurationDto dto = convertToDto(config);
            result.put(config.getConfigKey(), dto);
        }

        return result;
    }

    /**
     * Get configuration by key.
     */
    @Cacheable(value = "systemConfiguration", key = "#configKey")
    public SystemConfigurationDto getConfiguration(String configKey) {
        return repository.findByConfigKey(configKey)
                .map(this::convertToDto)
                .orElse(null);
    }

    /**
     * Update or create configuration.
     */
    @CacheEvict(value = "systemConfiguration", allEntries = true)
    public SystemConfigurationDto saveConfiguration(String configKey, JsonNode configValue, SystemConfiguration.ConfigurationType configType) {
        SystemConfiguration config = repository.findByConfigKey(configKey)
                .orElse(new SystemConfiguration(configKey, configValue, configType));

        config.setConfigValue(configValue);
        config.setUpdatedAt(LocalDateTime.now());

        SystemConfiguration saved = repository.save(config);
        return convertToDto(saved);
    }

    /**
     * Update a specific field in a configuration.
     */
    @CacheEvict(value = "systemConfiguration", allEntries = true)
    public SystemConfigurationDto updateConfigurationField(String configKey, String fieldPath, JsonNode value) {
        SystemConfiguration config = repository.findByConfigKey(configKey)
                .orElseThrow(() -> new RuntimeException("Configuration not found: " + configKey));

        JsonNode currentValue = config.getConfigValue();
        if (currentValue instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) currentValue;
            String[] pathParts = fieldPath.split("\\.");

            if (pathParts.length == 1) {
                objectNode.set(fieldPath, value);
            } else {
                // Handle nested paths
                ObjectNode current = objectNode;
                for (int i = 0; i < pathParts.length - 1; i++) {
                    String part = pathParts[i];
                    if (!current.has(part) || !current.get(part).isObject()) {
                        current.set(part, objectMapper.createObjectNode());
                    }
                    current = (ObjectNode) current.get(part);
                }
                current.set(pathParts[pathParts.length - 1], value);
            }

            config.setConfigValue(objectNode);
            config.setUpdatedAt(LocalDateTime.now());
            SystemConfiguration saved = repository.save(config);
            return convertToDto(saved);
        }

        throw new RuntimeException("Configuration value is not an object: " + configKey);
    }

    /**
     * Get configurations by type.
     */
    @Cacheable(value = "systemConfiguration", key = "#configType")
    public List<SystemConfigurationDto> getConfigurationsByType(SystemConfiguration.ConfigurationType configType) {
        return repository.findByConfigTypeAndIsActiveTrue(configType)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Deactivate configuration.
     */
    @CacheEvict(value = "systemConfiguration", allEntries = true)
    public void deactivateConfiguration(String configKey) {
        SystemConfiguration config = repository.findByConfigKey(configKey)
                .orElseThrow(() -> new RuntimeException("Configuration not found: " + configKey));

        config.setIsActive(false);
        config.setUpdatedAt(LocalDateTime.now());
        repository.save(config);
    }

    /**
     * Convert entity to DTO.
     */
    private SystemConfigurationDto convertToDto(SystemConfiguration entity) {
        SystemConfigurationDto dto = new SystemConfigurationDto();
        dto.setId(entity.getId());
        dto.setConfigKey(entity.getConfigKey());
        dto.setConfigValue(entity.getConfigValue());
        dto.setConfigType(entity.getConfigType().toString());
        dto.setDescription(entity.getDescription());
        dto.setIsActive(entity.getIsActive());
        dto.setVersion(entity.getVersion());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}