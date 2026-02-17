package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.BulkImportStaging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BulkImportStagingRepository extends JpaRepository<BulkImportStaging, UUID> {

    /**
     * Find all staging records for a specific batch
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE s.importBatchId = :batchId ORDER BY s.rowNumber")
    List<BulkImportStaging> findByImportBatchId(@Param("batchId") UUID batchId);

    /**
     * Find staging records for a batch with pagination
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE s.importBatchId = :batchId ORDER BY s.rowNumber")
    Page<BulkImportStaging> findByImportBatchId(@Param("batchId") UUID batchId, Pageable pageable);

    /**
     * Find staging records by validation status
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE s.importBatchId = :batchId AND s.validationStatus = :status ORDER BY s.rowNumber")
    List<BulkImportStaging> findByBatchIdAndValidationStatus(@Param("batchId") UUID batchId,
                                                              @Param("status") BulkImportStaging.ValidationStatus status);

    /**
     * Find staging records by processing status
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE s.importBatchId = :batchId AND s.processingStatus = :status ORDER BY s.rowNumber")
    List<BulkImportStaging> findByBatchIdAndProcessingStatus(@Param("batchId") UUID batchId,
                                                              @Param("status") BulkImportStaging.ProcessingStatus status);

    /**
     * Find staging records requiring manual review
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE s.importBatchId = :batchId AND s.requiresManualReview = true ORDER BY s.rowNumber")
    List<BulkImportStaging> findByBatchIdRequiringReview(@Param("batchId") UUID batchId);

    /**
     * Find staging records requiring manual review across all batches
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE s.requiresManualReview = true ORDER BY s.createdAt DESC")
    Page<BulkImportStaging> findAllRequiringReview(Pageable pageable);

    /**
     * Find staging record by natural key within a batch (for duplicate detection)
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE s.importBatchId = :batchId AND s.naturalKey = :naturalKey")
    Optional<BulkImportStaging> findByBatchIdAndNaturalKey(@Param("batchId") UUID batchId,
                                                           @Param("naturalKey") String naturalKey);

    /**
     * Find staging records by change request ID
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE s.changeRequestId = :changeRequestId ORDER BY s.rowNumber")
    List<BulkImportStaging> findByChangeRequestId(@Param("changeRequestId") UUID changeRequestId);

    /**
     * Find staging records with validation errors
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE s.importBatchId = :batchId AND s.validationStatus = 'INVALID' ORDER BY s.rowNumber")
    List<BulkImportStaging> findValidationErrors(@Param("batchId") UUID batchId);

    /**
     * Find staging records with validation warnings
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE s.importBatchId = :batchId AND s.validationStatus = 'WARNING' ORDER BY s.rowNumber")
    List<BulkImportStaging> findValidationWarnings(@Param("batchId") UUID batchId);

    /**
     * Find staging records with processing errors
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE s.importBatchId = :batchId AND s.processingStatus = 'FAILED' ORDER BY s.rowNumber")
    List<BulkImportStaging> findProcessingErrors(@Param("batchId") UUID batchId);

    /**
     * Find staging records ready for processing (valid and pending)
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE " +
           "s.importBatchId = :batchId AND " +
           "s.validationStatus = 'VALID' AND " +
           "s.processingStatus = 'PENDING' " +
           "ORDER BY s.rowNumber")
    List<BulkImportStaging> findReadyForProcessing(@Param("batchId") UUID batchId);

    /**
     * Count staging records by validation status for a batch
     */
    @Query("SELECT COUNT(s) FROM BulkImportStaging s WHERE s.importBatchId = :batchId AND s.validationStatus = :status")
    long countByBatchIdAndValidationStatus(@Param("batchId") UUID batchId,
                                           @Param("status") BulkImportStaging.ValidationStatus status);

    /**
     * Count staging records by processing status for a batch
     */
    @Query("SELECT COUNT(s) FROM BulkImportStaging s WHERE s.importBatchId = :batchId AND s.processingStatus = :status")
    long countByBatchIdAndProcessingStatus(@Param("batchId") UUID batchId,
                                           @Param("status") BulkImportStaging.ProcessingStatus status);

    /**
     * Get validation summary for a batch
     */
    @Query("SELECT " +
           "s.validationStatus, " +
           "COUNT(s) " +
           "FROM BulkImportStaging s WHERE s.importBatchId = :batchId " +
           "GROUP BY s.validationStatus")
    List<Object[]> getValidationSummary(@Param("batchId") UUID batchId);

    /**
     * Get processing summary for a batch
     */
    @Query("SELECT " +
           "s.processingStatus, " +
           "COUNT(s) " +
           "FROM BulkImportStaging s WHERE s.importBatchId = :batchId " +
           "GROUP BY s.processingStatus")
    List<Object[]> getProcessingSummary(@Param("batchId") UUID batchId);

    /**
     * Find staging records by target record ID (for conflict detection)
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE s.targetRecordId = :targetRecordId ORDER BY s.createdAt")
    List<BulkImportStaging> findByTargetRecordId(@Param("targetRecordId") UUID targetRecordId);

    /**
     * Update validation status for a record
     */
    @Modifying
    @Query("UPDATE BulkImportStaging s SET " +
           "s.validationStatus = :status, " +
           "s.validationErrors = :errors, " +
           "s.validationWarnings = :warnings, " +
           "s.validatedAt = :validatedAt, " +
           "s.validatedBy = :validatedBy " +
           "WHERE s.id = :id")
    int updateValidationStatus(@Param("id") UUID id,
                              @Param("status") BulkImportStaging.ValidationStatus status,
                              @Param("errors") String errors,
                              @Param("warnings") String warnings,
                              @Param("validatedAt") LocalDateTime validatedAt,
                              @Param("validatedBy") String validatedBy);

    /**
     * Update processing status for a record
     */
    @Modifying
    @Query("UPDATE BulkImportStaging s SET " +
           "s.processingStatus = :status, " +
           "s.processingErrors = :errors, " +
           "s.processedAt = :processedAt, " +
           "s.processedBy = :processedBy, " +
           "s.targetRecordId = :targetRecordId, " +
           "s.targetVersion = :targetVersion " +
           "WHERE s.id = :id")
    int updateProcessingStatus(@Param("id") UUID id,
                              @Param("status") BulkImportStaging.ProcessingStatus status,
                              @Param("errors") String errors,
                              @Param("processedAt") LocalDateTime processedAt,
                              @Param("processedBy") String processedBy,
                              @Param("targetRecordId") UUID targetRecordId,
                              @Param("targetVersion") Long targetVersion);

    /**
     * Batch update validation status for multiple records
     */
    @Modifying
    @Query("UPDATE BulkImportStaging s SET " +
           "s.validationStatus = :status, " +
           "s.validatedAt = :validatedAt, " +
           "s.validatedBy = :validatedBy " +
           "WHERE s.id IN :ids")
    int batchUpdateValidationStatus(@Param("ids") List<UUID> ids,
                                   @Param("status") BulkImportStaging.ValidationStatus status,
                                   @Param("validatedAt") LocalDateTime validatedAt,
                                   @Param("validatedBy") String validatedBy);

    /**
     * Batch update processing status for multiple records
     */
    @Modifying
    @Query("UPDATE BulkImportStaging s SET " +
           "s.processingStatus = :status, " +
           "s.processedAt = :processedAt, " +
           "s.processedBy = :processedBy " +
           "WHERE s.id IN :ids")
    int batchUpdateProcessingStatus(@Param("ids") List<UUID> ids,
                                   @Param("status") BulkImportStaging.ProcessingStatus status,
                                   @Param("processedAt") LocalDateTime processedAt,
                                   @Param("processedBy") String processedBy);

    /**
     * Delete staging records for a completed batch (cleanup)
     */
    @Modifying
    @Query("DELETE FROM BulkImportStaging s WHERE s.importBatchId = :batchId")
    int deleteByBatchId(@Param("batchId") UUID batchId);

    /**
     * Find duplicate natural keys within a batch
     */
    @Query("SELECT s.naturalKey, COUNT(s) FROM BulkImportStaging s " +
           "WHERE s.importBatchId = :batchId " +
           "GROUP BY s.naturalKey " +
           "HAVING COUNT(s) > 1")
    List<Object[]> findDuplicateNaturalKeys(@Param("batchId") UUID batchId);

    /**
     * Find records with high quality scores
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE " +
           "s.importBatchId = :batchId AND " +
           "s.dataQualityScore >= :minScore " +
           "ORDER BY s.dataQualityScore DESC")
    List<BulkImportStaging> findHighQualityRecords(@Param("batchId") UUID batchId,
                                                   @Param("minScore") Double minScore);

    /**
     * Find records with low confidence scores that may need review
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE " +
           "s.importBatchId = :batchId AND " +
           "s.confidenceScore < :maxScore " +
           "ORDER BY s.confidenceScore ASC")
    List<BulkImportStaging> findLowConfidenceRecords(@Param("batchId") UUID batchId,
                                                     @Param("maxScore") Double maxScore);

    /**
     * Find staging records by data type and operation type
     */
    @Query("SELECT s FROM BulkImportStaging s WHERE " +
           "s.dataType = :dataType AND " +
           "s.operationType = :operationType " +
           "ORDER BY s.createdAt DESC")
    Page<BulkImportStaging> findByDataTypeAndOperationType(@Param("dataType") BulkImportStaging.DataType dataType,
                                                           @Param("operationType") BulkImportStaging.OperationType operationType,
                                                           Pageable pageable);
}