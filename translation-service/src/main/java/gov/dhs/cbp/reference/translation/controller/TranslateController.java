package gov.dhs.cbp.reference.translation.controller;

import gov.dhs.cbp.reference.translation.dto.TranslationRequest;
import gov.dhs.cbp.reference.translation.dto.TranslationResponse;
import gov.dhs.cbp.reference.translation.dto.BatchTranslationRequest;
import gov.dhs.cbp.reference.translation.dto.BatchTranslationResponse;
import gov.dhs.cbp.reference.translation.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/translate")
@Tag(name = "Translation", description = "Code translation and crosswalk operations")
public class TranslateController {
    
    private final TranslationService translationService;
    
    public TranslateController(TranslationService translationService) {
        this.translationService = translationService;
    }
    
    @GetMapping
    @Operation(summary = "Translate a single code",
               description = "Translate a code from one system to another with optional temporal context")
    @Cacheable(value = "translations", 
               key = "#fromSystem + '-' + #fromCode + '-' + #toSystem + '-' + (#asOf != null ? #asOf.toString() : 'current')")
    public ResponseEntity<TranslationResponse> translate(
            @RequestParam String fromSystem,
            @RequestParam String fromCode,
            @RequestParam String toSystem,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        
        TranslationResponse response = translationService.translate(fromSystem, fromCode, toSystem, asOf);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/batch")
    @Operation(summary = "Translate multiple codes",
               description = "Batch translate multiple codes between systems")
    public ResponseEntity<BatchTranslationResponse> translateBatch(
            @Valid @RequestBody BatchTranslationRequest request) {
        
        BatchTranslationResponse response = translationService.translateBatch(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/systems")
    @Operation(summary = "Get available code systems",
               description = "List all code systems available for translation")
    public ResponseEntity<List<String>> getAvailableSystems() {
        List<String> systems = translationService.getAvailableCodeSystems();
        return ResponseEntity.ok(systems);
    }
    
    @GetMapping("/mappings")
    @Operation(summary = "Get all mappings for a code",
               description = "Retrieve all available mappings for a specific code")
    public ResponseEntity<List<TranslationResponse>> getAllMappings(
            @RequestParam String system,
            @RequestParam String code) {
        
        List<TranslationResponse> mappings = translationService.getAllMappingsForCode(system, code);
        return ResponseEntity.ok(mappings);
    }
    
    @GetMapping("/reverse")
    @Operation(summary = "Reverse translate a code",
               description = "Find source codes that map to a target code")
    public ResponseEntity<List<TranslationResponse>> reverseTranslate(
            @RequestParam String toSystem,
            @RequestParam String toCode,
            @RequestParam(required = false) String fromSystem) {
        
        List<TranslationResponse> responses = translationService.reverseTranslate(toSystem, toCode, fromSystem);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/deprecated")
    @Operation(summary = "Check if a mapping is deprecated",
               description = "Check deprecation status and get replacement suggestions")
    public ResponseEntity<TranslationResponse> checkDeprecation(
            @RequestParam String fromSystem,
            @RequestParam String fromCode,
            @RequestParam String toSystem) {
        
        TranslationResponse response = translationService.checkDeprecation(fromSystem, fromCode, toSystem);
        return ResponseEntity.ok(response);
    }
}