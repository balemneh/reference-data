package gov.dhs.cbp.reference.core.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CodeMappingTest {

    private CodeMapping codeMapping;
    private CodeSystem fromSystem;
    private CodeSystem toSystem;

    @BeforeEach
    void setUp() {
        codeMapping = new CodeMapping();
        
        fromSystem = new CodeSystem();
        fromSystem.setId(UUID.randomUUID());
        fromSystem.setCode("ISO3166-1");
        fromSystem.setName("ISO Country Codes Alpha-2");
        
        toSystem = new CodeSystem();
        toSystem.setId(UUID.randomUUID());
        toSystem.setCode("CBP-COUNTRY5");
        toSystem.setName("CBP 5-digit Country Codes");
    }

    @Test
    void testFromSystemGetterAndSetter() {
        codeMapping.setFromSystem(fromSystem);
        assertEquals(fromSystem, codeMapping.getFromSystem());
        assertEquals("ISO3166-1", codeMapping.getFromSystem().getCode());
    }

    @Test
    void testFromCodeGetterAndSetter() {
        String fromCode = "US";
        codeMapping.setFromCode(fromCode);
        assertEquals(fromCode, codeMapping.getFromCode());
    }

    @Test
    void testToSystemGetterAndSetter() {
        codeMapping.setToSystem(toSystem);
        assertEquals(toSystem, codeMapping.getToSystem());
        assertEquals("CBP-COUNTRY5", codeMapping.getToSystem().getCode());
    }

    @Test
    void testToCodeGetterAndSetter() {
        String toCode = "84001";
        codeMapping.setToCode(toCode);
        assertEquals(toCode, codeMapping.getToCode());
    }

    @Test
    void testRuleIdGetterAndSetter() {
        String ruleId = "COUNTRY_MAPPING_RULE_001";
        codeMapping.setRuleId(ruleId);
        assertEquals(ruleId, codeMapping.getRuleId());
    }

    @Test
    void testConfidenceDefaultAndGetterSetter() {
        // Test default value
        assertEquals(BigDecimal.valueOf(100), codeMapping.getConfidence());
        
        // Test setter
        BigDecimal confidence = new BigDecimal("85.50");
        codeMapping.setConfidence(confidence);
        assertEquals(confidence, codeMapping.getConfidence());
        
        // Test zero confidence
        BigDecimal zeroConfidence = BigDecimal.ZERO;
        codeMapping.setConfidence(zeroConfidence);
        assertEquals(zeroConfidence, codeMapping.getConfidence());
    }

    @Test
    void testMappingTypeGetterAndSetter() {
        String mappingType = "EXACT";
        codeMapping.setMappingType(mappingType);
        assertEquals(mappingType, codeMapping.getMappingType());
        
        // Test different mapping types
        String[] mappingTypes = {"EXACT", "PARTIAL", "DERIVED", "APPROXIMATE"};
        for (String type : mappingTypes) {
            codeMapping.setMappingType(type);
            assertEquals(type, codeMapping.getMappingType());
        }
    }

    @Test
    void testIsDeprecatedDefaultAndGetterSetter() {
        // Test default value
        assertFalse(codeMapping.getIsDeprecated());
        
        // Test setter
        codeMapping.setIsDeprecated(true);
        assertTrue(codeMapping.getIsDeprecated());
        
        codeMapping.setIsDeprecated(false);
        assertFalse(codeMapping.getIsDeprecated());
    }

    @Test
    void testDeprecationReasonGetterAndSetter() {
        String deprecationReason = "Code replaced by newer standard";
        codeMapping.setDeprecationReason(deprecationReason);
        assertEquals(deprecationReason, codeMapping.getDeprecationReason());
    }

    @Test
    void testNullFieldHandling() {
        // Test that optional fields can be null
        codeMapping.setRuleId(null);
        codeMapping.setMappingType(null);
        codeMapping.setDeprecationReason(null);
        
        assertNull(codeMapping.getRuleId());
        assertNull(codeMapping.getMappingType());
        assertNull(codeMapping.getDeprecationReason());
    }

    @Test
    void testInheritedBitemporalFields() {
        // Test that CodeMapping inherits bitemporal fields
        UUID id = UUID.randomUUID();
        LocalDate validFrom = LocalDate.now();
        LocalDate validTo = LocalDate.now().plusYears(1);
        LocalDateTime recordedAt = LocalDateTime.now();
        String recordedBy = "test-user";
        Long version = 1L;
        Boolean isCorrection = true;
        String changeRequestId = "CR-12345";
        String metadata = "{\"source\": \"manual mapping\"}";

        codeMapping.setId(id);
        codeMapping.setValidFrom(validFrom);
        codeMapping.setValidTo(validTo);
        codeMapping.setRecordedAt(recordedAt);
        codeMapping.setRecordedBy(recordedBy);
        codeMapping.setVersion(version);
        codeMapping.setIsCorrection(isCorrection);
        codeMapping.setChangeRequestId(changeRequestId);
        codeMapping.setMetadata(metadata);

        assertEquals(id, codeMapping.getId());
        assertEquals(validFrom, codeMapping.getValidFrom());
        assertEquals(validTo, codeMapping.getValidTo());
        assertEquals(recordedAt, codeMapping.getRecordedAt());
        assertEquals(recordedBy, codeMapping.getRecordedBy());
        assertEquals(version, codeMapping.getVersion());
        assertEquals(isCorrection, codeMapping.getIsCorrection());
        assertEquals(changeRequestId, codeMapping.getChangeRequestId());
        assertEquals(metadata, codeMapping.getMetadata());
    }

    @Test
    void testCodeMappingWithCompleteData() {
        // Create a complete code mapping object with all fields
        codeMapping.setFromSystem(fromSystem);
        codeMapping.setFromCode("US");
        codeMapping.setToSystem(toSystem);
        codeMapping.setToCode("84001");
        codeMapping.setRuleId("COUNTRY_MAPPING_RULE_001");
        codeMapping.setConfidence(new BigDecimal("95.00"));
        codeMapping.setMappingType("EXACT");
        codeMapping.setIsDeprecated(false);
        codeMapping.setDeprecationReason(null);

        // Verify all fields are set correctly
        assertEquals(fromSystem, codeMapping.getFromSystem());
        assertEquals("US", codeMapping.getFromCode());
        assertEquals(toSystem, codeMapping.getToSystem());
        assertEquals("84001", codeMapping.getToCode());
        assertEquals("COUNTRY_MAPPING_RULE_001", codeMapping.getRuleId());
        assertEquals(new BigDecimal("95.00"), codeMapping.getConfidence());
        assertEquals("EXACT", codeMapping.getMappingType());
        assertFalse(codeMapping.getIsDeprecated());
        assertNull(codeMapping.getDeprecationReason());
    }

    @Test
    void testCodeMappingValidityChecks() {
        // Set up a code mapping with validity dates
        codeMapping.setValidFrom(LocalDate.of(2024, 1, 1));
        codeMapping.setValidTo(LocalDate.of(2024, 12, 31));

        // Test validity on specific dates
        assertTrue(codeMapping.wasValidOn(LocalDate.of(2024, 6, 15)));
        assertFalse(codeMapping.wasValidOn(LocalDate.of(2023, 12, 31)));
        assertFalse(codeMapping.wasValidOn(LocalDate.of(2025, 1, 1)));

        // Test open-ended validity
        codeMapping.setValidTo(null);
        assertTrue(codeMapping.wasValidOn(LocalDate.of(2025, 1, 1)));
    }

    @Test
    void testConfidenceEdgeCases() {
        // Test minimum confidence
        BigDecimal minConfidence = BigDecimal.ZERO;
        codeMapping.setConfidence(minConfidence);
        assertEquals(minConfidence, codeMapping.getConfidence());

        // Test maximum confidence
        BigDecimal maxConfidence = new BigDecimal("100.00");
        codeMapping.setConfidence(maxConfidence);
        assertEquals(maxConfidence, codeMapping.getConfidence());

        // Test confidence with high precision
        BigDecimal preciseConfidence = new BigDecimal("87.55");
        codeMapping.setConfidence(preciseConfidence);
        assertEquals(preciseConfidence, codeMapping.getConfidence());
    }

    @Test
    void testDeprecatedCodeMapping() {
        // Test deprecated mapping scenario
        codeMapping.setIsDeprecated(true);
        codeMapping.setDeprecationReason("Replaced by new international standard");
        
        assertTrue(codeMapping.getIsDeprecated());
        assertEquals("Replaced by new international standard", codeMapping.getDeprecationReason());
    }

    @Test
    void testBidirectionalMapping() {
        // Create a mapping from A to B
        codeMapping.setFromSystem(fromSystem);
        codeMapping.setFromCode("US");
        codeMapping.setToSystem(toSystem);
        codeMapping.setToCode("84001");
        
        // Create reverse mapping from B to A
        CodeMapping reverseMapping = new CodeMapping();
        reverseMapping.setFromSystem(toSystem);
        reverseMapping.setFromCode("84001");
        reverseMapping.setToSystem(fromSystem);
        reverseMapping.setToCode("US");
        
        // Verify both mappings
        assertEquals(fromSystem, codeMapping.getFromSystem());
        assertEquals("US", codeMapping.getFromCode());
        assertEquals(toSystem, codeMapping.getToSystem());
        assertEquals("84001", codeMapping.getToCode());
        
        assertEquals(toSystem, reverseMapping.getFromSystem());
        assertEquals("84001", reverseMapping.getFromCode());
        assertEquals(fromSystem, reverseMapping.getToSystem());
        assertEquals("US", reverseMapping.getToCode());
    }

    @Test
    void testCodeMappingDefaultValues() {
        CodeMapping newMapping = new CodeMapping();
        
        // Test default values
        assertEquals(BigDecimal.valueOf(100), newMapping.getConfidence());
        assertFalse(newMapping.getIsDeprecated());
        assertNull(newMapping.getFromSystem());
        assertNull(newMapping.getFromCode());
        assertNull(newMapping.getToSystem());
        assertNull(newMapping.getToCode());
        assertNull(newMapping.getRuleId());
        assertNull(newMapping.getMappingType());
        assertNull(newMapping.getDeprecationReason());
    }

    @Test
    void testCodeMappingSystemsNullHandling() {
        // Test null code systems
        codeMapping.setFromSystem(null);
        codeMapping.setToSystem(null);
        
        assertNull(codeMapping.getFromSystem());
        assertNull(codeMapping.getToSystem());
    }

    @Test
    void testLongDeprecationReason() {
        // Test long deprecation reason text
        String longReason = "This mapping has been deprecated due to changes in international standards " +
                           "and the introduction of new code systems that provide better granularity " +
                           "and improved accuracy for cross-border trade operations.";
        
        codeMapping.setDeprecationReason(longReason);
        assertEquals(longReason, codeMapping.getDeprecationReason());
    }
}