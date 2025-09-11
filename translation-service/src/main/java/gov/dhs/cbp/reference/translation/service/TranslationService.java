package gov.dhs.cbp.reference.translation.service;

import gov.dhs.cbp.reference.core.entity.CodeMapping;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.repository.CodeMappingRepository;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import gov.dhs.cbp.reference.translation.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TranslationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);
    
    private final CodeMappingRepository codeMappingRepository;
    private final CodeSystemRepository codeSystemRepository;
    
    public TranslationService(CodeMappingRepository codeMappingRepository,
                              CodeSystemRepository codeSystemRepository) {
        this.codeMappingRepository = codeMappingRepository;
        this.codeSystemRepository = codeSystemRepository;
    }
    
    public TranslationResponse translate(String fromSystem, String fromCode, 
                                        String toSystem, LocalDate asOf) {
        logger.debug("Translating {} {} to {} as of {}", fromSystem, fromCode, toSystem, asOf);
        
        List<CodeMapping> mappings;
        if (asOf != null) {
            mappings = codeMappingRepository.findMappingAsOf(fromSystem, fromCode, toSystem, asOf);
        } else {
            mappings = codeMappingRepository.findCurrentMapping(fromSystem, fromCode, toSystem);
        }
        
        if (mappings.isEmpty()) {
            TranslationResponse response = new TranslationResponse();
            response.setFromSystem(fromSystem);
            response.setFromCode(fromCode);
            response.setToSystem(toSystem);
            return response;
        }
        
        CodeMapping bestMapping = mappings.stream()
                .max((m1, m2) -> m1.getConfidence().compareTo(m2.getConfidence()))
                .orElse(mappings.get(0));
        
        return mapToResponse(bestMapping);
    }
    
    public BatchTranslationResponse translateBatch(BatchTranslationRequest request) {
        BatchTranslationResponse response = new BatchTranslationResponse();
        response.setTotalRequested(request.getTranslations().size());
        
        for (TranslationRequest translationRequest : request.getTranslations()) {
            try {
                TranslationResponse translation = translate(
                        translationRequest.getFromSystem(),
                        translationRequest.getFromCode(),
                        translationRequest.getToSystem(),
                        translationRequest.getAsOf()
                );
                
                if (translation.getToCode() != null) {
                    response.getSuccessful().add(translation);
                } else {
                    BatchTranslationResponse.TranslationError error = new BatchTranslationResponse.TranslationError();
                    error.setFromSystem(translationRequest.getFromSystem());
                    error.setFromCode(translationRequest.getFromCode());
                    error.setToSystem(translationRequest.getToSystem());
                    error.setErrorMessage("No mapping found");
                    error.setErrorCode("NO_MAPPING");
                    response.getFailed().add(error);
                }
            } catch (Exception e) {
                logger.error("Error translating {} {} to {}: {}", 
                        translationRequest.getFromSystem(),
                        translationRequest.getFromCode(),
                        translationRequest.getToSystem(),
                        e.getMessage());
                
                if (request.isFailOnError()) {
                    throw new RuntimeException("Batch translation failed", e);
                }
                
                BatchTranslationResponse.TranslationError error = new BatchTranslationResponse.TranslationError();
                error.setFromSystem(translationRequest.getFromSystem());
                error.setFromCode(translationRequest.getFromCode());
                error.setToSystem(translationRequest.getToSystem());
                error.setErrorMessage(e.getMessage());
                error.setErrorCode("TRANSLATION_ERROR");
                response.getFailed().add(error);
            }
        }
        
        response.setSuccessCount(response.getSuccessful().size());
        response.setFailureCount(response.getFailed().size());
        
        return response;
    }
    
    @Cacheable(value = "codeSystems")
    public List<String> getAvailableCodeSystems() {
        return codeSystemRepository.findAll().stream()
                .map(CodeSystem::getCode)
                .sorted()
                .collect(Collectors.toList());
    }
    
    public List<TranslationResponse> getAllMappingsForCode(String system, String code) {
        List<TranslationResponse> responses = new ArrayList<>();
        
        for (String targetSystem : getAvailableCodeSystems()) {
            if (!targetSystem.equals(system)) {
                List<CodeMapping> mappings = codeMappingRepository.findCurrentMapping(system, code, targetSystem);
                responses.addAll(mappings.stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()));
            }
        }
        
        return responses;
    }
    
    public List<TranslationResponse> reverseTranslate(String toSystem, String toCode, String fromSystem) {
        List<CodeMapping> mappings = codeMappingRepository.findAllCurrent();
        
        return mappings.stream()
                .filter(m -> m.getToSystem().getCode().equals(toSystem) && 
                            m.getToCode().equals(toCode) &&
                            (fromSystem == null || m.getFromSystem().getCode().equals(fromSystem)))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public TranslationResponse checkDeprecation(String fromSystem, String fromCode, String toSystem) {
        List<CodeMapping> mappings = codeMappingRepository.findCurrentMapping(fromSystem, fromCode, toSystem);
        
        if (mappings.isEmpty()) {
            TranslationResponse response = new TranslationResponse();
            response.setFromSystem(fromSystem);
            response.setFromCode(fromCode);
            response.setToSystem(toSystem);
            return response;
        }
        
        CodeMapping mapping = mappings.get(0);
        TranslationResponse response = mapToResponse(mapping);
        
        if (mapping.getIsDeprecated()) {
            List<CodeMapping> alternatives = codeMappingRepository
                    .findCurrentMapping(fromSystem, fromCode, toSystem).stream()
                    .filter(m -> !m.getIsDeprecated())
                    .toList();
            
            if (!alternatives.isEmpty()) {
                response.setAlternativeCodes(alternatives.stream()
                        .map(CodeMapping::getToCode)
                        .collect(Collectors.toList()));
            }
        }
        
        return response;
    }
    
    private TranslationResponse mapToResponse(CodeMapping mapping) {
        TranslationResponse response = new TranslationResponse();
        response.setFromSystem(mapping.getFromSystem().getCode());
        response.setFromCode(mapping.getFromCode());
        response.setToSystem(mapping.getToSystem().getCode());
        response.setToCode(mapping.getToCode());
        response.setConfidence(mapping.getConfidence());
        response.setMappingType(mapping.getMappingType());
        response.setValidFrom(mapping.getValidFrom());
        response.setValidTo(mapping.getValidTo());
        response.setDeprecated(mapping.getIsDeprecated());
        response.setDeprecationReason(mapping.getDeprecationReason());
        response.setRuleId(mapping.getRuleId());
        return response;
    }
}