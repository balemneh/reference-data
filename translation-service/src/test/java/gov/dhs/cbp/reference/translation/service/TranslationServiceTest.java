package gov.dhs.cbp.reference.translation.service;

import gov.dhs.cbp.reference.core.entity.CodeMapping;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.repository.CodeMappingRepository;
import gov.dhs.cbp.reference.core.repository.CodeSystemRepository;
import gov.dhs.cbp.reference.translation.dto.BatchTranslationRequest;
import gov.dhs.cbp.reference.translation.dto.BatchTranslationResponse;
import gov.dhs.cbp.reference.translation.dto.TranslationRequest;
import gov.dhs.cbp.reference.translation.dto.TranslationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private CodeMappingRepository codeMappingRepository;

    @Mock
    private CodeSystemRepository codeSystemRepository;

    @InjectMocks
    private TranslationService translationService;

    private CodeMapping codeMapping;
    private CodeSystem fromSystem;
    private CodeSystem toSystem;

    @BeforeEach
    void setUp() {
        fromSystem = new CodeSystem();
        fromSystem.setId(UUID.randomUUID());
        fromSystem.setCode("ISO3166-1");
        fromSystem.setName("ISO Country Codes");
        
        toSystem = new CodeSystem();
        toSystem.setId(UUID.randomUUID());
        toSystem.setCode("CBP-COUNTRY5");
        toSystem.setName("CBP Country Codes");
        
        codeMapping = new CodeMapping();
        codeMapping.setId(UUID.randomUUID());
        codeMapping.setFromSystem(fromSystem);
        codeMapping.setFromCode("USA");
        codeMapping.setToSystem(toSystem);
        codeMapping.setToCode("US");
        codeMapping.setConfidence(BigDecimal.valueOf(100));
        codeMapping.setValidFrom(LocalDate.now().minusYears(1));
        codeMapping.setValidTo(null);
        codeMapping.setIsDeprecated(false);
    }

    @Test
    void testTranslate() {
        when(codeMappingRepository.findCurrentMapping(
                eq("ISO3166-1"), eq("USA"), eq("CBP-COUNTRY5")))
                .thenReturn(Arrays.asList(codeMapping));

        TranslationResponse response = translationService.translate(
                "ISO3166-1", "USA", "CBP-COUNTRY5", null);

        assertNotNull(response);
        assertEquals("USA", response.getFromCode());
        assertEquals("US", response.getToCode());
        assertEquals("ISO3166-1", response.getFromSystem());
        assertEquals("CBP-COUNTRY5", response.getToSystem());
        assertEquals(BigDecimal.valueOf(100), response.getConfidence());
    }

    @Test
    void testTranslateNotFound() {
        when(codeMappingRepository.findCurrentMapping(
                eq("ISO3166-1"), eq("XXX"), eq("CBP-COUNTRY5")))
                .thenReturn(Arrays.asList());

        TranslationResponse response = translationService.translate(
                "ISO3166-1", "XXX", "CBP-COUNTRY5", null);

        assertNotNull(response);
        assertNull(response.getToCode());
        assertEquals("ISO3166-1", response.getFromSystem());
        assertEquals("XXX", response.getFromCode());
        assertEquals("CBP-COUNTRY5", response.getToSystem());
    }

    @Test
    void testTranslateAsOf() {
        LocalDate asOf = LocalDate.now();
        when(codeMappingRepository.findMappingAsOf(
                eq("ISO3166-1"), eq("USA"), eq("CBP-COUNTRY5"), eq(asOf)))
                .thenReturn(Arrays.asList(codeMapping));

        TranslationResponse response = translationService.translate(
                "ISO3166-1", "USA", "CBP-COUNTRY5", asOf);

        assertNotNull(response);
        assertEquals("US", response.getToCode());
    }

    @Test
    void testBatchTranslate() {
        TranslationRequest request1 = new TranslationRequest();
        request1.setFromSystem("ISO3166-1");
        request1.setFromCode("USA");
        request1.setToSystem("CBP-COUNTRY5");
        
        TranslationRequest request2 = new TranslationRequest();
        request2.setFromSystem("ISO3166-1");
        request2.setFromCode("CAN");
        request2.setToSystem("CBP-COUNTRY5");
        
        BatchTranslationRequest batchRequest = new BatchTranslationRequest();
        batchRequest.setTranslations(Arrays.asList(request1, request2));
        
        CodeMapping mapping2 = new CodeMapping();
        mapping2.setFromCode("CAN");
        mapping2.setToCode("CA");
        mapping2.setFromSystem(fromSystem);
        mapping2.setToSystem(toSystem);
        mapping2.setConfidence(BigDecimal.valueOf(100));
        
        when(codeMappingRepository.findCurrentMapping(
                eq("ISO3166-1"), eq("USA"), eq("CBP-COUNTRY5")))
                .thenReturn(Arrays.asList(codeMapping));
        when(codeMappingRepository.findCurrentMapping(
                eq("ISO3166-1"), eq("CAN"), eq("CBP-COUNTRY5")))
                .thenReturn(Arrays.asList(mapping2));

        BatchTranslationResponse response = translationService.translateBatch(batchRequest);

        assertNotNull(response);
        assertEquals(2, response.getSuccessful().size());
        assertEquals(2, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
    }

    @Test
    void testGetAvailableCodeSystems() {
        CodeSystem system1 = new CodeSystem();
        system1.setCode("ISO3166-1");
        
        CodeSystem system2 = new CodeSystem();
        system2.setCode("CBP-COUNTRY5");
        
        when(codeSystemRepository.findAll())
                .thenReturn(Arrays.asList(system1, system2));

        List<String> systems = translationService.getAvailableCodeSystems();

        assertNotNull(systems);
        assertEquals(2, systems.size());
        assertTrue(systems.contains("ISO3166-1"));
        assertTrue(systems.contains("CBP-COUNTRY5"));
    }

    @Test
    void testGetAllMappingsForCode() {
        when(codeSystemRepository.findAll())
                .thenReturn(Arrays.asList(fromSystem, toSystem));
        when(codeMappingRepository.findCurrentMapping(
                eq("ISO3166-1"), eq("USA"), eq("CBP-COUNTRY5")))
                .thenReturn(Arrays.asList(codeMapping));

        List<TranslationResponse> mappings = translationService.getAllMappingsForCode("ISO3166-1", "USA");

        assertNotNull(mappings);
        assertFalse(mappings.isEmpty());
    }

    @Test
    void testReverseTranslate() {
        when(codeMappingRepository.findAllCurrent())
                .thenReturn(Arrays.asList(codeMapping));

        List<TranslationResponse> mappings = translationService.reverseTranslate("CBP-COUNTRY5", "US", "ISO3166-1");

        assertNotNull(mappings);
        assertEquals(1, mappings.size());
        assertEquals("USA", mappings.get(0).getFromCode());
    }

    @Test
    void testCheckDeprecation() {
        codeMapping.setIsDeprecated(true);
        codeMapping.setDeprecationReason("Replaced with new code");
        
        when(codeMappingRepository.findCurrentMapping(
                eq("ISO3166-1"), eq("USA"), eq("CBP-COUNTRY5")))
                .thenReturn(Arrays.asList(codeMapping));

        TranslationResponse response = translationService.checkDeprecation("ISO3166-1", "USA", "CBP-COUNTRY5");

        assertNotNull(response);
        assertTrue(response.isDeprecated());
        assertEquals("Replaced with new code", response.getDeprecationReason());
    }
}