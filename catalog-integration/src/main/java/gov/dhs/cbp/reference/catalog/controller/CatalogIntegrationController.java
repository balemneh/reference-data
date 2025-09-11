package gov.dhs.cbp.reference.catalog.controller;

import gov.dhs.cbp.reference.catalog.model.*;
import gov.dhs.cbp.reference.catalog.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for catalog integration and translation generation
 */
@RestController
@RequestMapping("/api/v1/catalog-integration")
@Tag(name = "Catalog Integration", description = "Manage consumer registrations and generate translations")
public class CatalogIntegrationController {
    
    private static final Logger log = LoggerFactory.getLogger(CatalogIntegrationController.class);
    
    private final ConsumerRegistrationService registrationService;
    private final TranslationGeneratorService generatorService;
    private final SchemaAnalyzer schemaAnalyzer;
    
    public CatalogIntegrationController(ConsumerRegistrationService registrationService,
                                       TranslationGeneratorService generatorService,
                                       SchemaAnalyzer schemaAnalyzer) {
        this.registrationService = registrationService;
        this.generatorService = generatorService;
        this.schemaAnalyzer = schemaAnalyzer;
    }
    
    /**
     * Register a consumer table for reference data translation
     */
    @PostMapping("/registrations")
    @Operation(summary = "Register a consumer table",
              description = "Register a downstream consumer table with reference data mapping")
    public Mono<ResponseEntity<ConsumerRegistration>> registerTable(
            @Valid @RequestBody ConsumerTableRequest request) {
        
        log.info("Received registration request from consumer {} for table {}", 
                request.getConsumerId(), request.getTableFqn());
        
        return registrationService.registerConsumerTable(request)
                .map(registration -> ResponseEntity.status(HttpStatus.CREATED).body(registration))
                .onErrorResume(error -> {
                    log.error("Failed to register table", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
                });
    }
    
    /**
     * Get all registrations for a consumer
     */
    @GetMapping("/registrations/consumer/{consumerId}")
    @Operation(summary = "Get consumer registrations",
              description = "Retrieve all table registrations for a specific consumer")
    public Flux<ConsumerRegistration> getConsumerRegistrations(@PathVariable String consumerId) {
        log.info("Fetching registrations for consumer: {}", consumerId);
        return registrationService.getConsumerRegistrations(consumerId);
    }
    
    /**
     * Get suggested mapping for a table
     */
    @GetMapping("/mappings/suggest")
    @Operation(summary = "Suggest column mappings",
              description = "Analyze table schema and suggest mappings to reference data")
    public Mono<TableMapping> suggestMapping(
            @RequestParam String tableFqn,
            @RequestParam String referenceType) {
        
        log.info("Suggesting mapping for table {} to {}", tableFqn, referenceType);
        
        ReferenceDataType refType = ReferenceDataType.fromCode(referenceType);
        return registrationService.suggestMapping(tableFqn, refType);
    }
    
    /**
     * Validate a mapping configuration
     */
    @PostMapping("/mappings/validate")
    @Operation(summary = "Validate mapping",
              description = "Validate that a mapping configuration is correct")
    public Mono<ValidationResult> validateMapping(
            @RequestParam String tableFqn,
            @RequestParam String referenceType,
            @RequestBody TableMapping mapping) {
        
        log.info("Validating mapping for table {} to {}", tableFqn, referenceType);
        
        ReferenceDataType refType = ReferenceDataType.fromCode(referenceType);
        return registrationService.validateMapping(tableFqn, refType, mapping);
    }
    
    /**
     * Generate translation artifact for a registration
     */
    @PostMapping("/translations/generate/{consumerId}/{tableFqn}")
    @Operation(summary = "Generate translation",
              description = "Generate SQL translation artifact for a registered table")
    public Mono<TranslationArtifact> generateTranslation(
            @PathVariable String consumerId,
            @PathVariable String tableFqn) {
        
        log.info("Generating translation for consumer {} table {}", consumerId, tableFqn);
        
        return registrationService.getConsumerRegistrations(consumerId)
                .filter(reg -> reg.getTableFqn().equals(tableFqn))
                .next()
                .flatMap(generatorService::generateTranslation);
    }
    
    /**
     * Generate all translations for a consumer
     */
    @PostMapping("/translations/generate-batch/{consumerId}")
    @Operation(summary = "Generate batch translations",
              description = "Generate SQL translations for all registered tables of a consumer")
    public Mono<ResponseEntity<Object>> generateBatchTranslations(@PathVariable String consumerId) {
        
        log.info("Generating batch translations for consumer: {}", consumerId);
        
        return generatorService.generateBatchTranslations(consumerId)
                .map(artifacts -> ResponseEntity.ok((Object) artifacts))
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }
    
    /**
     * Update mapping for a registered table
     */
    @PutMapping("/registrations/{consumerId}/{tableFqn}/mapping")
    @Operation(summary = "Update mapping",
              description = "Update the column mapping for a registered table")
    public Mono<ConsumerRegistration> updateMapping(
            @PathVariable String consumerId,
            @PathVariable String tableFqn,
            @RequestBody TableMapping mapping) {
        
        log.info("Updating mapping for consumer {} table {}", consumerId, tableFqn);
        
        return registrationService.updateMapping(consumerId, tableFqn, mapping);
    }
    
    /**
     * Deactivate a registration
     */
    @DeleteMapping("/registrations/{consumerId}/{tableFqn}")
    @Operation(summary = "Deactivate registration",
              description = "Deactivate a consumer table registration")
    public Mono<ResponseEntity<Void>> deactivateRegistration(
            @PathVariable String consumerId,
            @PathVariable String tableFqn) {
        
        log.info("Deactivating registration for consumer {} table {}", consumerId, tableFqn);
        
        return registrationService.deactivateRegistration(consumerId, tableFqn)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
    
    /**
     * Get all consumers subscribed to a reference type
     */
    @GetMapping("/subscriptions/reference-type/{referenceType}")
    @Operation(summary = "Get subscribers",
              description = "Get all consumers subscribed to a specific reference data type")
    public Flux<ConsumerRegistration> getSubscribers(@PathVariable String referenceType) {
        
        log.info("Fetching subscribers for reference type: {}", referenceType);
        
        ReferenceDataType refType = ReferenceDataType.fromCode(referenceType);
        return registrationService.getSubscribersForReferenceType(refType);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check",
              description = "Check the health of the catalog integration service")
    public Mono<ResponseEntity<Object>> health() {
        return Mono.just(ResponseEntity.ok()
                .body(new HealthResponse("Catalog Integration Service", "UP")));
    }
    
    /**
     * Health response DTO
     */
    private static class HealthResponse {
        private final String service;
        private final String status;
        
        public HealthResponse(String service, String status) {
            this.service = service;
            this.status = status;
        }
        
        public String getService() {
            return service;
        }
        
        public String getStatus() {
            return status;
        }
    }
}