package gov.dhs.cbp.reference.loader.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiffDetectorTest {
    
    private DiffDetector<TestStagingEntity, TestProductionEntity> diffDetector;
    
    @BeforeEach
    void setUp() {
        diffDetector = new DiffDetector<>(
            staging -> staging.getCode(),
            production -> production.getCode(),
            (staging, production) -> !staging.getName().equals(production.getName())
        );
    }
    
    @Test
    void testDetectAdditions() {
        List<TestStagingEntity> staging = Arrays.asList(
            createStaging("A001", "Item A"),
            createStaging("A002", "Item B"),
            createStaging("A003", "Item C")
        );
        
        List<TestProductionEntity> production = Arrays.asList(
            createProduction("A001", "Item A")
        );
        
        DiffResult<TestStagingEntity, TestProductionEntity> result = 
            diffDetector.detectDifferences(staging, production);
        
        assertEquals(2, result.getAdditions().size());
        assertEquals(0, result.getUpdates().size());
        assertEquals(0, result.getDeletions().size());
        assertEquals(1, result.getUnchanged().size());
    }
    
    @Test
    void testDetectUpdates() {
        List<TestStagingEntity> staging = Arrays.asList(
            createStaging("A001", "Item A Updated"),
            createStaging("A002", "Item B")
        );
        
        List<TestProductionEntity> production = Arrays.asList(
            createProduction("A001", "Item A"),
            createProduction("A002", "Item B")
        );
        
        DiffResult<TestStagingEntity, TestProductionEntity> result = 
            diffDetector.detectDifferences(staging, production);
        
        assertEquals(0, result.getAdditions().size());
        assertEquals(1, result.getUpdates().size());
        assertEquals(0, result.getDeletions().size());
        assertEquals(1, result.getUnchanged().size());
    }
    
    @Test
    void testDetectDeletions() {
        List<TestStagingEntity> staging = Arrays.asList(
            createStaging("A001", "Item A")
        );
        
        List<TestProductionEntity> production = Arrays.asList(
            createProduction("A001", "Item A"),
            createProduction("A002", "Item B"),
            createProduction("A003", "Item C")
        );
        
        DiffResult<TestStagingEntity, TestProductionEntity> result = 
            diffDetector.detectDifferences(staging, production);
        
        assertEquals(0, result.getAdditions().size());
        assertEquals(0, result.getUpdates().size());
        assertEquals(2, result.getDeletions().size());
        assertEquals(1, result.getUnchanged().size());
    }
    
    @Test
    void testEmptyStaging() {
        List<TestStagingEntity> staging = Arrays.asList();
        List<TestProductionEntity> production = Arrays.asList(
            createProduction("A001", "Item A")
        );
        
        DiffResult<TestStagingEntity, TestProductionEntity> result = 
            diffDetector.detectDifferences(staging, production);
        
        assertEquals(0, result.getAdditions().size());
        assertEquals(1, result.getDeletions().size());
        assertTrue(result.hasChanges());
    }
    
    @Test
    void testEmptyProduction() {
        List<TestStagingEntity> staging = Arrays.asList(
            createStaging("A001", "Item A")
        );
        List<TestProductionEntity> production = Arrays.asList();
        
        DiffResult<TestStagingEntity, TestProductionEntity> result = 
            diffDetector.detectDifferences(staging, production);
        
        assertEquals(1, result.getAdditions().size());
        assertEquals(0, result.getDeletions().size());
        assertTrue(result.hasChanges());
    }
    
    @Test
    void testNoChanges() {
        List<TestStagingEntity> staging = Arrays.asList(
            createStaging("A001", "Item A"),
            createStaging("A002", "Item B")
        );
        
        List<TestProductionEntity> production = Arrays.asList(
            createProduction("A001", "Item A"),
            createProduction("A002", "Item B")
        );
        
        DiffResult<TestStagingEntity, TestProductionEntity> result = 
            diffDetector.detectDifferences(staging, production);
        
        assertFalse(result.hasChanges());
        assertEquals(2, result.getUnchanged().size());
    }
    
    private TestStagingEntity createStaging(String code, String name) {
        TestStagingEntity entity = new TestStagingEntity();
        entity.setCode(code);
        entity.setName(name);
        return entity;
    }
    
    private TestProductionEntity createProduction(String code, String name) {
        TestProductionEntity entity = new TestProductionEntity();
        entity.setCode(code);
        entity.setName(name);
        return entity;
    }
    
    // Test entities
    static class TestStagingEntity extends StagingEntity {
        private String code;
        private String name;
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    static class TestProductionEntity {
        private String code;
        private String name;
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}