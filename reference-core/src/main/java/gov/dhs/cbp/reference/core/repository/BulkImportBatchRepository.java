package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.BulkImportBatch;
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
public interface BulkImportBatchRepository extends JpaRepository<BulkImportBatch, UUID> {

    /**
     * Find batches by status
     */
    @Query("SELECT b FROM BulkImportBatch b WHERE b.status = :status ORDER BY b.createdAt DESC")
    Page<BulkImportBatch> findByStatus(@Param("status") BulkImportBatch.BatchStatus status, Pageable pageable);

    /**
     * Find batches by change request ID
     */
    @Query("SELECT b FROM BulkImportBatch b WHERE b.changeRequestId = :changeRequestId ORDER BY b.createdAt DESC")
    List<BulkImportBatch> findByChangeRequestId(@Param("changeRequestId") UUID changeRequestId);

    /**
     * Find batches by data type
     */
    @Query("SELECT b FROM BulkImportBatch b WHERE b.dataType = :dataType ORDER BY b.createdAt DESC")
    Page<BulkImportBatch> findByDataType(@Param("dataType") BulkImportBatch.DataType dataType, Pageable pageable);

    /**
     * Find batches by source system
     */
    @Query("SELECT b FROM BulkImportBatch b WHERE b.sourceSystem = :sourceSystem ORDER BY b.createdAt DESC")
    Page<BulkImportBatch> findBySourceSystem(@Param("sourceSystem") String sourceSystem, Pageable pageable);

    /**
     * Find batches created by a specific user
     */
    @Query("SELECT b FROM BulkImportBatch b WHERE b.createdBy = :createdBy ORDER BY b.createdAt DESC")
    Page<BulkImportBatch> findByCreatedBy(@Param("createdBy") String createdBy, Pageable pageable);

    /**
     * Find batches created within a date range
     */
    @Query("SELECT b FROM BulkImportBatch b WHERE b.createdAt >= :startDate AND b.createdAt <= :endDate ORDER BY b.createdAt DESC")
    Page<BulkImportBatch> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 Pageable pageable);

    /**
     * Find batches with specific filters
     */
    @Query("SELECT b FROM BulkImportBatch b WHERE " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:dataType IS NULL OR b.dataType = :dataType) AND " +
           "(:sourceSystem IS NULL OR b.sourceSystem = :sourceSystem) AND " +
           "(:createdBy IS NULL OR b.createdBy = :createdBy) AND " +
           "b.createdAt >= :fromDate " +
           "ORDER BY b.createdAt DESC")
    Page<BulkImportBatch> findByFilters(@Param("status") BulkImportBatch.BatchStatus status,
                                       @Param("dataType") BulkImportBatch.DataType dataType,
                                       @Param("sourceSystem") String sourceSystem,
                                       @Param("createdBy") String createdBy,
                                       @Param("fromDate") LocalDateTime fromDate,
                                       Pageable pageable);

    /**
     * Find batches that are in progress (VALIDATING or PROCESSING)
     */
    @Query("SELECT b FROM BulkImportBatch b WHERE b.status IN ('VALIDATING', 'PROCESSING') ORDER BY b.startedAt")
    List<BulkImportBatch> findInProgressBatches();

    /**
     * Find failed batches that might need retry
     */
    @Query("SELECT b FROM BulkImportBatch b WHERE b.status = 'FAILED' ORDER BY b.completedAt DESC")
    List<BulkImportBatch> findFailedBatches();

    /**
     * Find stale batches (started but not completed after a certain time)
     */
    @Query("SELECT b FROM BulkImportBatch b WHERE " +
           "b.status IN ('VALIDATING', 'PROCESSING') AND " +
           "b.startedAt < :staleThreshold")
    List<BulkImportBatch> findStaleBatches(@Param("staleThreshold") LocalDateTime staleThreshold);

    /**
     * Find batch by source file checksum to detect duplicates
     */
    @Query("SELECT b FROM BulkImportBatch b WHERE b.sourceFileChecksum = :checksum AND b.status != 'FAILED'")
    Optional<BulkImportBatch> findBySourceFileChecksum(@Param("checksum") String checksum);

    /**
     * Count batches by status
     */
    @Query("SELECT COUNT(b) FROM BulkImportBatch b WHERE b.status = :status")
    long countByStatus(@Param("status") BulkImportBatch.BatchStatus status);

    /**
     * Count batches by data type
     */
    @Query("SELECT COUNT(b) FROM BulkImportBatch b WHERE b.dataType = :dataType")
    long countByDataType(@Param("dataType") BulkImportBatch.DataType dataType);

    /**
     * Get summary statistics for batches
     */
    @Query("SELECT " +
           "COUNT(b) as totalBatches, " +
           "SUM(b.totalRecords) as totalRecords, " +
           "SUM(b.recordsProcessed) as totalProcessed, " +
           "SUM(b.recordsValid) as totalValid, " +
           "SUM(b.recordsInvalid) as totalInvalid " +
           "FROM BulkImportBatch b WHERE b.createdAt >= :fromDate")
    Object[] getBatchStatistics(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Update batch progress - use only when needed for performance
     */
    @Modifying
    @Query("UPDATE BulkImportBatch b SET " +
           "b.recordsProcessed = :recordsProcessed, " +
           "b.recordsValid = :recordsValid, " +
           "b.recordsInvalid = :recordsInvalid, " +
           "b.recordsWarnings = :recordsWarnings, " +
           "b.recordsSkipped = :recordsSkipped " +
           "WHERE b.id = :batchId")
    int updateBatchProgress(@Param("batchId") UUID batchId,
                           @Param("recordsProcessed") Integer recordsProcessed,
                           @Param("recordsValid") Integer recordsValid,
                           @Param("recordsInvalid") Integer recordsInvalid,
                           @Param("recordsWarnings") Integer recordsWarnings,
                           @Param("recordsSkipped") Integer recordsSkipped);

    /**
     * Update batch status and completion time
     */
    @Modifying
    @Query("UPDATE BulkImportBatch b SET " +
           "b.status = :status, " +
           "b.completedAt = :completedAt, " +
           "b.durationSeconds = :durationSeconds " +
           "WHERE b.id = :batchId")
    int updateBatchCompletion(@Param("batchId") UUID batchId,
                             @Param("status") BulkImportBatch.BatchStatus status,
                             @Param("completedAt") LocalDateTime completedAt,
                             @Param("durationSeconds") Integer durationSeconds);

    /**
     * Find batches requiring notification
     */
    @Query("SELECT b FROM BulkImportBatch b WHERE " +
           "b.status IN ('COMPLETED', 'FAILED') AND " +
           "b.notificationSent = false AND " +
           "b.completedAt < :notificationThreshold")
    List<BulkImportBatch> findBatchesRequiringNotification(@Param("notificationThreshold") LocalDateTime notificationThreshold);

    /**
     * Mark notification as sent
     */
    @Modifying
    @Query("UPDATE BulkImportBatch b SET b.notificationSent = true WHERE b.id = :batchId")
    int markNotificationSent(@Param("batchId") UUID batchId);
}