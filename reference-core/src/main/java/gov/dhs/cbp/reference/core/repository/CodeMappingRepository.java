package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.CodeMapping;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodeMappingRepository extends BitemporalRepository<CodeMapping> {
    
    @Query("SELECT m FROM CodeMapping m " +
           "WHERE m.fromSystem.code = :fromSystem AND m.fromCode = :fromCode " +
           "AND m.toSystem.code = :toSystem " +
           "AND (m.validTo IS NULL OR m.validTo > CURRENT_DATE) " +
           "AND m.isDeprecated = false")
    List<CodeMapping> findCurrentMapping(@Param("fromSystem") String fromSystem,
                                         @Param("fromCode") String fromCode,
                                         @Param("toSystem") String toSystem);
    
    @Query("SELECT m FROM CodeMapping m " +
           "WHERE m.fromSystem.code = :fromSystem AND m.fromCode = :fromCode " +
           "AND m.toSystem.code = :toSystem AND m.toCode = :toCode " +
           "AND (m.validTo IS NULL OR m.validTo > CURRENT_DATE)")
    Optional<CodeMapping> findExactMapping(@Param("fromSystem") String fromSystem,
                                           @Param("fromCode") String fromCode,
                                           @Param("toSystem") String toSystem,
                                           @Param("toCode") String toCode);
    
    @Query("SELECT m FROM CodeMapping m " +
           "WHERE m.fromSystem.code = :fromSystem AND m.fromCode = :fromCode " +
           "AND m.toSystem.code = :toSystem " +
           "AND m.validFrom <= :asOfDate " +
           "AND (m.validTo IS NULL OR m.validTo > :asOfDate) " +
           "AND m.isDeprecated = false")
    List<CodeMapping> findMappingAsOf(@Param("fromSystem") String fromSystem,
                                      @Param("fromCode") String fromCode,
                                      @Param("toSystem") String toSystem,
                                      @Param("asOfDate") LocalDate asOfDate);
    
    @Query("SELECT m FROM CodeMapping m " +
           "WHERE m.ruleId = :ruleId " +
           "AND (m.validTo IS NULL OR m.validTo > CURRENT_DATE)")
    List<CodeMapping> findByRuleId(@Param("ruleId") String ruleId);
    
    @Query("SELECT m FROM CodeMapping m " +
           "WHERE m.isDeprecated = true " +
           "AND (m.validTo IS NULL OR m.validTo > CURRENT_DATE)")
    List<CodeMapping> findDeprecatedMappings();
    
    @Query("SELECT DISTINCT m.fromSystem.code FROM CodeMapping m " +
           "WHERE m.toSystem.code = :toSystem")
    List<String> findSourceSystemsForTarget(@Param("toSystem") String toSystem);
}