package gov.dhs.cbp.reference.catalog.service;

import gov.dhs.cbp.reference.catalog.client.OpenMetadataClient;
import gov.dhs.cbp.reference.catalog.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing consumer registrations and their table mappings
 * to reference data types.
 */
@Service
public class ConsumerRegistrationService {
    
    private static final Logger log = LoggerFactory.getLogger(ConsumerRegistrationService.class);
    
    private final OpenMetadataClient metadataClient;
    private final SchemaAnalyzer schemaAnalyzer;
    private final Map<String, ConsumerRegistration> registrations = new ConcurrentHashMap<>();
    
    public ConsumerRegistrationService(OpenMetadataClient metadataClient, 
                                      SchemaAnalyzer schemaAnalyzer) {
        this.metadataClient = metadataClient;
        this.schemaAnalyzer = schemaAnalyzer;
    }
    
    /**
     * Register a consumer's table with reference data mapping
     */
    public Mono<ConsumerRegistration> registerConsumerTable(ConsumerTableRequest request) {
        log.info("Registering consumer table: {} for consumer: {}", 
                request.getTableFqn(), request.getConsumerId());
        
        return metadataClient.getDatasetMetadata(request.getTableFqn())
                .flatMap(metadata -> {
                    // Analyze schema to suggest mappings if not provided
                    TableMapping mapping = request.getMapping();
                    if (mapping == null || mapping.getColumnMappings().isEmpty()) {
                        mapping = schemaAnalyzer.analyzeAndSuggestMapping(
                            metadata, 
                            request.getReferenceDataType()
                        );
                    }
                    
                    // Create registration
                    ConsumerRegistration registration = new ConsumerRegistration();
                    registration.setConsumerId(request.getConsumerId());
                    registration.setTableFqn(request.getTableFqn());
                    registration.setReferenceDataType(request.getReferenceDataType());
                    registration.setMapping(mapping);
                    registration.setPreferences(request.getPreferences());
                    registration.setRegisteredAt(LocalDateTime.now());
                    registration.setActive(true);
                    
                    // Store custom properties in OpenMetadata
                    return updateMetadataProperties(registration)
                            .then(Mono.just(registration));
                })
                .doOnSuccess(reg -> {
                    registrations.put(reg.getConsumerId() + ":" + reg.getTableFqn(), reg);
                    log.info("Successfully registered table {} for consumer {}", 
                            reg.getTableFqn(), reg.getConsumerId());
                });
    }
    
    /**
     * Get all registrations for a consumer
     */
    public Flux<ConsumerRegistration> getConsumerRegistrations(String consumerId) {
        return Flux.fromIterable(registrations.values())
                .filter(reg -> reg.getConsumerId().equals(consumerId));
    }
    
    /**
     * Get all consumers subscribed to a reference data type
     */
    public Flux<ConsumerRegistration> getSubscribersForReferenceType(ReferenceDataType type) {
        return Flux.fromIterable(registrations.values())
                .filter(reg -> reg.getReferenceDataType().equals(type))
                .filter(ConsumerRegistration::isActive);
    }
    
    /**
     * Update column mappings for a registered table
     */
    public Mono<ConsumerRegistration> updateMapping(String consumerId, 
                                                    String tableFqn, 
                                                    TableMapping newMapping) {
        String key = consumerId + ":" + tableFqn;
        ConsumerRegistration registration = registrations.get(key);
        
        if (registration == null) {
            return Mono.error(new IllegalArgumentException(
                "No registration found for consumer " + consumerId + " and table " + tableFqn));
        }
        
        registration.setMapping(newMapping);
        registration.setLastUpdated(LocalDateTime.now());
        
        return updateMetadataProperties(registration)
                .then(Mono.just(registration));
    }
    
    /**
     * Deactivate a consumer registration
     */
    public Mono<Void> deactivateRegistration(String consumerId, String tableFqn) {
        String key = consumerId + ":" + tableFqn;
        ConsumerRegistration registration = registrations.get(key);
        
        if (registration != null) {
            registration.setActive(false);
            registration.setDeactivatedAt(LocalDateTime.now());
            log.info("Deactivated registration for consumer {} table {}", 
                    consumerId, tableFqn);
        }
        
        return Mono.empty();
    }
    
    /**
     * Update OpenMetadata custom properties with registration info
     */
    private Mono<Void> updateMetadataProperties(ConsumerRegistration registration) {
        Map<String, Object> customProperties = new HashMap<>();
        customProperties.put("reference_data_type", registration.getReferenceDataType().toString());
        customProperties.put("reference_data_consumer", registration.getConsumerId());
        customProperties.put("translation_config", registration.getMapping().toJson());
        customProperties.put("translation_preferences", registration.getPreferences().toJson());
        customProperties.put("registration_timestamp", registration.getRegisteredAt().toString());
        
        return metadataClient.updateTableCustomProperties(
            registration.getTableFqn(), 
            customProperties
        );
    }
    
    /**
     * Validate that a table can be mapped to a reference data type
     */
    public Mono<ValidationResult> validateMapping(String tableFqn, 
                                                  ReferenceDataType referenceType,
                                                  TableMapping mapping) {
        return metadataClient.getDatasetMetadata(tableFqn)
                .map(metadata -> schemaAnalyzer.validateMapping(metadata, referenceType, mapping));
    }
    
    /**
     * Get suggested mappings for a table based on schema analysis
     */
    public Mono<TableMapping> suggestMapping(String tableFqn, ReferenceDataType referenceType) {
        return metadataClient.getDatasetMetadata(tableFqn)
                .map(metadata -> schemaAnalyzer.analyzeAndSuggestMapping(metadata, referenceType));
    }
}