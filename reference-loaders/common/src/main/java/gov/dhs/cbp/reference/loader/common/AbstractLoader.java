package gov.dhs.cbp.reference.loader.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Abstract base class for all reference data loaders.
 * Provides common functionality for loading, staging, validation, and diff detection.
 *
 * @param <S> Source data type
 * @param <T> Target entity type
 * @param <ST> Staging table entity type
 */
public abstract class AbstractLoader<S, T, ST extends StagingEntity> {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected final JobLauncher jobLauncher;
    protected final JobRepository jobRepository;
    protected final PlatformTransactionManager transactionManager;
    protected final LoaderConfiguration configuration;
    protected final ValidationService<S> validationService;
    protected final DiffDetector<ST, T> diffDetector;
    
    protected AbstractLoader(
            JobLauncher jobLauncher,
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            LoaderConfiguration configuration,
            ValidationService<S> validationService,
            DiffDetector<ST, T> diffDetector) {
        this.jobLauncher = jobLauncher;
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.configuration = configuration;
        this.validationService = validationService;
        this.diffDetector = diffDetector;
    }
    
    /**
     * Execute a full load process
     */
    public LoaderResult executeLoad(LoaderContext context) {
        String executionId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        LoaderResult result = new LoaderResult(executionId, getLoaderName());
        
        logger.info("Starting {} load execution: {}", getLoaderName(), executionId);
        
        try {
            // Step 1: Extract data from source
            logger.info("Extracting data from source...");
            List<S> sourceData = extractData(context);
            result.setRecordsRead(sourceData.size());
            
            // Step 2: Validate source data
            logger.info("Validating {} records...", sourceData.size());
            ValidationResult validationResult = validationService.validate(sourceData);
            result.setValidationErrors(validationResult.getErrors());
            
            if (!validationResult.isValid() && configuration.isFailOnValidationError()) {
                throw new LoaderException("Validation failed with " + 
                    validationResult.getErrors().size() + " errors");
            }
            
            // Step 3: Transform to staging entities
            logger.info("Transforming data to staging format...");
            List<ST> stagingData = transformToStaging(sourceData, validationResult);
            result.setRecordsStaged(stagingData.size());
            
            // Step 4: Load into staging tables
            logger.info("Loading {} records into staging...", stagingData.size());
            loadToStaging(stagingData, executionId);
            
            // Step 5: Detect differences
            logger.info("Detecting differences between staging and production...");
            DiffResult<ST, T> diffResult = diffDetector.detectDifferences(
                stagingData, 
                getCurrentProductionData()
            );
            
            result.setRecordsAdded(diffResult.getAdditions().size());
            result.setRecordsUpdated(diffResult.getUpdates().size());
            result.setRecordsDeleted(diffResult.getDeletions().size());
            
            // Step 6: Apply changes if auto-apply is enabled
            if (configuration.isAutoApplyChanges() && diffResult.hasChanges()) {
                logger.info("Applying {} changes to production...", diffResult.getTotalChanges());
                applyChanges(diffResult, context);
                result.setChangesApplied(true);
            } else if (diffResult.hasChanges()) {
                logger.info("Changes detected but auto-apply is disabled. Creating change request...");
                String changeRequestId = createChangeRequest(diffResult, context);
                result.setChangeRequestId(changeRequestId);
            }
            
            // Step 7: Publish events
            if (configuration.isPublishEvents() && diffResult.hasChanges()) {
                publishEvents(diffResult, executionId);
            }
            
            result.setStatus(LoaderStatus.SUCCESS);
            logger.info("Load completed successfully: {}", result);
            
        } catch (Exception e) {
            logger.error("Load failed for execution: " + executionId, e);
            result.setStatus(LoaderStatus.FAILED);
            result.setErrorMessage(e.getMessage());
            handleLoadFailure(e, context);
        } finally {
            result.setEndTime(LocalDateTime.now());
            result.setDurationMillis(
                java.time.Duration.between(startTime, result.getEndTime()).toMillis()
            );
            saveLoaderResult(result);
        }
        
        return result;
    }
    
    /**
     * Execute an incremental load for changes since last run
     */
    public LoaderResult executeIncrementalLoad(LoaderContext context) {
        context.setIncrementalMode(true);
        LocalDateTime lastRunTime = getLastSuccessfulRunTime();
        context.setLastRunTime(lastRunTime);
        
        logger.info("Executing incremental load since: {}", lastRunTime);
        return executeLoad(context);
    }
    
    @Transactional
    protected void loadToStaging(List<ST> stagingData, String executionId) {
        // Clear existing staging data if full load
        if (!configuration.isIncrementalMode()) {
            clearStagingTables();
        }
        
        // Batch insert staging records
        int batchSize = configuration.getBatchSize();
        for (int i = 0; i < stagingData.size(); i += batchSize) {
            int end = Math.min(i + batchSize, stagingData.size());
            List<ST> batch = stagingData.subList(i, end);
            
            batch.forEach(record -> {
                record.setLoadExecutionId(executionId);
                record.setLoadedAt(LocalDateTime.now());
            });
            
            saveStagingBatch(batch);
            logger.debug("Saved staging batch {}-{}", i, end);
        }
    }
    
    @Transactional
    protected void applyChanges(DiffResult<ST, T> diffResult, LoaderContext context) {
        // Apply additions
        for (ST addition : diffResult.getAdditions()) {
            T entity = transformToEntity(addition);
            saveEntity(entity, context);
        }
        
        // Apply updates
        for (DiffResult.UpdatePair<ST, T> update : diffResult.getUpdates()) {
            T updated = updateEntity(update.getCurrent(), update.getStaged());
            saveEntity(updated, context);
        }
        
        // Apply deletions (soft delete in bitemporal model)
        for (T deletion : diffResult.getDeletions()) {
            markAsDeleted(deletion, context);
        }
    }
    
    // Abstract methods to be implemented by specific loaders
    protected abstract String getLoaderName();
    protected abstract List<S> extractData(LoaderContext context) throws Exception;
    protected abstract List<ST> transformToStaging(List<S> sourceData, ValidationResult validationResult);
    protected abstract T transformToEntity(ST stagingEntity);
    protected abstract T updateEntity(T current, ST staged);
    protected abstract void saveEntity(T entity, LoaderContext context);
    protected abstract void markAsDeleted(T entity, LoaderContext context);
    protected abstract List<T> getCurrentProductionData();
    protected abstract void saveStagingBatch(List<ST> batch);
    protected abstract void clearStagingTables();
    protected abstract void publishEvents(DiffResult<ST, T> diffResult, String executionId);
    protected abstract String createChangeRequest(DiffResult<ST, T> diffResult, LoaderContext context);
    protected abstract LocalDateTime getLastSuccessfulRunTime();
    protected abstract void saveLoaderResult(LoaderResult result);
    protected abstract void handleLoadFailure(Exception e, LoaderContext context);
}