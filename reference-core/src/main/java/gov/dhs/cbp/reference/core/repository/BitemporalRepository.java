package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.Bitemporal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface BitemporalRepository<T extends Bitemporal> extends JpaRepository<T, UUID> {
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.validTo IS NULL OR e.validTo > CURRENT_DATE")
    List<T> findAllCurrent();
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND (e.validTo IS NULL OR e.validTo > CURRENT_DATE)")
    Optional<T> findCurrentById(@Param("id") UUID id);
    
    @Query("SELECT e FROM #{#entityName} e WHERE " +
           "e.validFrom <= :asOfDate AND (e.validTo IS NULL OR e.validTo > :asOfDate)")
    List<T> findAllAsOf(@Param("asOfDate") LocalDate asOfDate);
    
    @Query("SELECT e FROM #{#entityName} e WHERE " +
           "e.validFrom <= :asOfDate AND (e.validTo IS NULL OR e.validTo > :asOfDate) " +
           "AND e.recordedAt <= :recordedAsOf")
    List<T> findAllBitemporal(@Param("asOfDate") LocalDate asOfDate, 
                              @Param("recordedAsOf") LocalDateTime recordedAsOf);
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.changeRequestId = :changeRequestId")
    List<T> findByChangeRequestId(@Param("changeRequestId") String changeRequestId);
    
    @Query("SELECT DISTINCT e.version FROM #{#entityName} e ORDER BY e.version DESC")
    List<Long> findAllVersions();
    
    @Query("SELECT MAX(e.version) FROM #{#entityName} e")
    Optional<Long> findLatestVersion();
}