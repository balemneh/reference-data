package gov.dhs.cbp.reference.loader.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AbstractLoaderIntegrationTest {
    
    @Mock
    private JobLauncher jobLauncher;
    
    @Mock
    private JobRepository jobRepository;
    
    @Mock
    private PlatformTransactionManager transactionManager;
    
    @Mock
    private LoaderConfiguration configuration;
    
    @Mock
    private Validator validator;
    
    private TestLoader testLoader;
    
    @BeforeEach
    void setUp() {
        ValidationService<TestSourceData> validationService = new ValidationService<>(validator);
        DiffDetector<TestStagingEntity, TestProductionEntity> diffDetector = 
            new DiffDetector<>(
                TestStagingEntity::getCode,
                TestProductionEntity::getCode,
                (staging, production) -> !staging.getName().equals(production.getName())
            );
        
        testLoader = new TestLoader(
            jobLauncher,
            jobRepository,
            transactionManager,
            configuration,
            validationService,
            diffDetector
        );
        
        lenient().when(configuration.getBatchSize()).thenReturn(100);
        lenient().when(configuration.isAutoApplyChanges()).thenReturn(true);
        lenient().when(configuration.isPublishEvents()).thenReturn(true);
        lenient().when(validator.validate(any())).thenReturn(new HashSet<>());
    }
    
    @Test
    void testCompleteLoadProcess() {
        LoaderContext context = new LoaderContext("test-exec", "test-user");
        
        LoaderResult result = testLoader.executeLoad(context);
        
        assertNotNull(result);
        assertEquals("TestLoader", result.getLoaderName());
        assertEquals(LoaderStatus.SUCCESS, result.getStatus());
        assertEquals(2, result.getRecordsRead());
        assertEquals(2, result.getRecordsStaged());
        assertTrue(result.isChangesApplied());
    }
    
    @Test
    void testIncrementalLoad() {
        when(configuration.isIncrementalMode()).thenReturn(true);
        
        LoaderContext context = new LoaderContext("incr-exec", "test-user");
        LoaderResult result = testLoader.executeIncrementalLoad(context);
        
        assertNotNull(result);
        assertTrue(context.isIncrementalMode());
        assertNotNull(context.getLastRunTime());
    }
    
    @Test
    void testValidationFailure() {
        when(configuration.isFailOnValidationError()).thenReturn(true);
        
        // Add a custom validation rule that always fails
        ValidationService<TestSourceData> validationService = new ValidationService<>(validator);
        validationService.addRule(new ValidationService.ValidationRule<TestSourceData>() {
            @Override
            public String getName() {
                return "AlwaysFail";
            }
            
            @Override
            public ValidationService.ValidationRule.Result validate(TestSourceData record) {
                return ValidationService.ValidationRule.Result.invalid("Test validation failure");
            }
        });
        
        // Create new test loader with failing validation service
        DiffDetector<TestStagingEntity, TestProductionEntity> diffDetector = 
            new DiffDetector<>(
                TestStagingEntity::getCode,
                TestProductionEntity::getCode,
                (staging, production) -> !staging.getName().equals(production.getName())
            );
        
        TestLoader failingLoader = new TestLoader(
            jobLauncher,
            jobRepository,
            transactionManager,
            configuration,
            validationService,
            diffDetector
        );
        
        LoaderContext context = new LoaderContext("fail-exec", "test-user");
        LoaderResult result = failingLoader.executeLoad(context);
        
        assertEquals(LoaderStatus.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Validation failed"));
    }
    
    @Test
    void testChangeRequestCreation() {
        when(configuration.isAutoApplyChanges()).thenReturn(false);
        
        LoaderContext context = new LoaderContext("cr-exec", "test-user");
        LoaderResult result = testLoader.executeLoad(context);
        
        assertNotNull(result.getChangeRequestId());
        assertFalse(result.isChangesApplied());
    }
    
    @Test
    void testLoadFailureHandling() {
        testLoader.shouldFailExtraction = true;
        
        LoaderContext context = new LoaderContext("error-exec", "test-user");
        LoaderResult result = testLoader.executeLoad(context);
        
        assertEquals(LoaderStatus.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Extraction failed"));
    }
    
    // Test implementation of AbstractLoader
    static class TestLoader extends AbstractLoader<TestSourceData, TestProductionEntity, TestStagingEntity> {
        
        public boolean shouldFailExtraction = false;
        private List<TestProductionEntity> productionData;
        
        public TestLoader(
                JobLauncher jobLauncher,
                JobRepository jobRepository,
                PlatformTransactionManager transactionManager,
                LoaderConfiguration configuration,
                ValidationService<TestSourceData> validationService,
                DiffDetector<TestStagingEntity, TestProductionEntity> diffDetector) {
            super(jobLauncher, jobRepository, transactionManager, configuration, validationService, diffDetector);
            
            // Initialize with some production data
            productionData = Arrays.asList(
                createProductionEntity("PROD1", "Production Item 1")
            );
        }
        
        @Override
        protected String getLoaderName() {
            return "TestLoader";
        }
        
        @Override
        protected List<TestSourceData> extractData(LoaderContext context) throws Exception {
            if (shouldFailExtraction) {
                throw new RuntimeException("Extraction failed");
            }
            
            return Arrays.asList(
                createSourceData("PROD1", "Production Item 1 Updated"),
                createSourceData("PROD2", "Production Item 2")
            );
        }
        
        @Override
        protected List<TestStagingEntity> transformToStaging(
                List<TestSourceData> sourceData, 
                ValidationResult validationResult) {
            
            return sourceData.stream()
                .map(this::mapToStaging)
                .collect(java.util.stream.Collectors.toList());
        }
        
        @Override
        protected TestProductionEntity transformToEntity(TestStagingEntity stagingEntity) {
            TestProductionEntity entity = new TestProductionEntity();
            entity.setCode(stagingEntity.getCode());
            entity.setName(stagingEntity.getName());
            return entity;
        }
        
        @Override
        protected TestProductionEntity updateEntity(TestProductionEntity current, TestStagingEntity staged) {
            TestProductionEntity updated = new TestProductionEntity();
            updated.setCode(current.getCode());
            updated.setName(staged.getName());
            return updated;
        }
        
        @Override
        protected void saveEntity(TestProductionEntity entity, LoaderContext context) {
            // Mock save operation
        }
        
        @Override
        protected void markAsDeleted(TestProductionEntity entity, LoaderContext context) {
            entity.setName(entity.getName() + " [DELETED]");
        }
        
        @Override
        protected List<TestProductionEntity> getCurrentProductionData() {
            return productionData;
        }
        
        @Override
        protected void saveStagingBatch(List<TestStagingEntity> batch) {
            // Mock save operation
        }
        
        @Override
        protected void clearStagingTables() {
            // Mock clear operation
        }
        
        @Override
        protected void publishEvents(DiffResult<TestStagingEntity, TestProductionEntity> diffResult, String executionId) {
            // Mock event publishing
        }
        
        @Override
        protected String createChangeRequest(DiffResult<TestStagingEntity, TestProductionEntity> diffResult, LoaderContext context) {
            return "CR-TEST-" + System.currentTimeMillis();
        }
        
        @Override
        protected LocalDateTime getLastSuccessfulRunTime() {
            return LocalDateTime.now().minusDays(1);
        }
        
        @Override
        protected void saveLoaderResult(LoaderResult result) {
            // Mock save operation
        }
        
        @Override
        protected void handleLoadFailure(Exception e, LoaderContext context) {
            // Mock failure handling
        }
        
        private TestSourceData createSourceData(String code, String name) {
            TestSourceData data = new TestSourceData();
            data.setCode(code);
            data.setName(name);
            return data;
        }
        
        private TestStagingEntity mapToStaging(TestSourceData source) {
            TestStagingEntity staging = new TestStagingEntity();
            staging.setCode(source.getCode());
            staging.setName(source.getName());
            return staging;
        }
        
        private TestProductionEntity createProductionEntity(String code, String name) {
            TestProductionEntity entity = new TestProductionEntity();
            entity.setCode(code);
            entity.setName(name);
            return entity;
        }
    }
    
    // Test data classes
    static class TestSourceData {
        private String code;
        private String name;
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
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