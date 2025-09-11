package gov.dhs.cbp.reference.loader.genc.repository;

import gov.dhs.cbp.reference.loader.common.StagingEntity;
import gov.dhs.cbp.reference.loader.genc.entity.GencEntityStaging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GENC entity staging table
 */
@Repository
public interface GencEntityStagingRepository extends JpaRepository<GencEntityStaging, Long> {

    /**
     * Find staging entity by GENC 3-character code
     */
    Optional<GencEntityStaging> findByChar3Code(String char3Code);

    /**
     * Find staging entity by GENC 2-character code
     */
    Optional<GencEntityStaging> findByChar2Code(String char2Code);

    /**
     * Find all staging entities by load execution ID
     */
    List<GencEntityStaging> findByLoadExecutionId(String loadExecutionId);

    /**
     * Find entities by GENC status
     */
    List<GencEntityStaging> findByGencStatus(String status);

    /**
     * Find entities by entity type
     */
    List<GencEntityStaging> findByEntityType(String entityType);

    /**
     * Find entities that have parent codes (dependencies)
     */
    @Query("SELECT g FROM GencEntityStaging g WHERE g.parentCode IS NOT NULL")
    List<GencEntityStaging> findDependencies();

    /**
     * Find entities by parent code
     */
    List<GencEntityStaging> findByParentCode(String parentCode);

    /**
     * Clear all staging data for a new full load
     */
    @Modifying
    @Query("DELETE FROM GencEntityStaging")
    void clearAll();

    /**
     * Clear staging data for a specific load execution
     */
    @Modifying
    @Query("DELETE FROM GencEntityStaging g WHERE g.loadExecutionId = :executionId")
    void clearByLoadExecutionId(@Param("executionId") String executionId);

    /**
     * Count entities by processing status
     */
    @Query("SELECT COUNT(g) FROM GencEntityStaging g WHERE g.processingStatus = :status")
    Long countByProcessingStatus(@Param("status") StagingEntity.ProcessingStatus status);

    /**
     * Find entities with validation errors
     */
    @Query("SELECT g FROM GencEntityStaging g WHERE g.validationStatus = 'INVALID' OR g.validationStatus = 'WARNING'")
    List<GencEntityStaging> findWithValidationIssues();

    /**
     * Check if an entity with the same 3-character code already exists in staging
     */
    boolean existsByChar3Code(String char3Code);

    /**
     * Find entities that need to be processed (pending status)
     */
    @Query("SELECT g FROM GencEntityStaging g WHERE g.processingStatus = 'PENDING' ORDER BY g.id")
    List<GencEntityStaging> findPendingProcessing();
}