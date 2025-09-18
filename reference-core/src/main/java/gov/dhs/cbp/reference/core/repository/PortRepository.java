package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.Port;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortRepository extends BitemporalRepository<Port> {
    
    @Query("SELECT p FROM Port p WHERE p.portCode = :portCode AND p.codeSystem.id = :systemId " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE) " +
           "ORDER BY p.version DESC")
    Optional<Port> findCurrentByPortCodeAndSystem(@Param("portCode") String portCode, 
                                                  @Param("systemId") UUID systemId);
    
    @Query("SELECT p FROM Port p WHERE p.portCode = :portCode AND p.codeSystem.code = :systemCode " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE) " +
           "ORDER BY p.version DESC")
    Optional<Port> findCurrentByPortCodeAndSystemCode(@Param("portCode") String portCode, 
                                                      @Param("systemCode") String systemCode);
    
    @Query("SELECT p FROM Port p WHERE p.unLocode = :unLocode AND p.codeSystem.code = :systemCode " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE) " +
           "ORDER BY p.version DESC")
    Optional<Port> findCurrentByUnLocodeAndSystemCode(@Param("unLocode") String unLocode, 
                                                      @Param("systemCode") String systemCode);
    
    @Query("SELECT p FROM Port p WHERE p.cbpPortCode = :cbpPortCode AND p.codeSystem.code = :systemCode " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE) " +
           "ORDER BY p.version DESC")
    Optional<Port> findCurrentByCbpPortCodeAndSystemCode(@Param("cbpPortCode") String cbpPortCode, 
                                                         @Param("systemCode") String systemCode);
    
    @Query("SELECT p FROM Port p WHERE p.codeSystem.code = :systemCode " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE) " +
           "ORDER BY p.portName")
    Page<Port> findCurrentBySystemCode(@Param("systemCode") String systemCode, Pageable pageable);
    
    @Query("SELECT p FROM Port p WHERE p.countryCode = :countryCode " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE)")
    List<Port> findCurrentByCountryCode(@Param("countryCode") String countryCode);
    
    @Query("SELECT p FROM Port p WHERE p.city = :city " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE)")
    List<Port> findCurrentByCity(@Param("city") String city);
    
    @Query("SELECT p FROM Port p WHERE p.portType = :portType " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE)")
    List<Port> findCurrentByPortType(@Param("portType") String portType);
    
    @Query("SELECT p FROM Port p WHERE p.portCode = :portCode " +
           "AND p.codeSystem.code = :systemCode " +
           "AND p.validFrom <= :asOfDate " +
           "AND (p.validTo IS NULL OR p.validTo > :asOfDate) " +
           "ORDER BY p.version DESC")
    Optional<Port> findByPortCodeAndSystemAsOf(@Param("portCode") String portCode,
                                               @Param("systemCode") String systemCode,
                                               @Param("asOfDate") LocalDate asOfDate);
    
    @Query("SELECT p FROM Port p WHERE p.unLocode = :unLocode " +
           "AND p.codeSystem.code = :systemCode " +
           "AND p.validFrom <= :asOfDate " +
           "AND (p.validTo IS NULL OR p.validTo > :asOfDate) " +
           "ORDER BY p.version DESC")
    Optional<Port> findByUnLocodeAndSystemAsOf(@Param("unLocode") String unLocode,
                                               @Param("systemCode") String systemCode,
                                               @Param("asOfDate") LocalDate asOfDate);
    
    @Query("SELECT p FROM Port p WHERE " +
           "LOWER(p.portName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE)")
    Page<Port> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT DISTINCT p.portCode FROM Port p WHERE p.codeSystem.code = :systemCode " +
           "ORDER BY p.portCode")
    List<String> findAllPortCodes(@Param("systemCode") String systemCode);
    
    @Query("SELECT DISTINCT p.unLocode FROM Port p WHERE p.codeSystem.code = :systemCode " +
           "AND p.unLocode IS NOT NULL " +
           "ORDER BY p.unLocode")
    List<String> findAllUnLocodes(@Param("systemCode") String systemCode);
    
    @Query("SELECT DISTINCT p.cbpPortCode FROM Port p WHERE p.codeSystem.code = :systemCode " +
           "AND p.cbpPortCode IS NOT NULL " +
           "ORDER BY p.cbpPortCode")
    List<String> findAllCbpPortCodes(@Param("systemCode") String systemCode);
    
    @Query("SELECT COUNT(p) FROM Port p WHERE p.isActive = true " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE)")
    long countByIsActiveTrue();
    
    @Query("SELECT p FROM Port p ORDER BY p.recordedAt DESC LIMIT 1")
    Optional<Port> findTopByOrderByRecordedAtDesc();
    
    // Additional methods for integration tests
    
    @Query("SELECT p FROM Port p WHERE p.portCode = :portCode AND p.codeSystem = :codeSystem " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE) " +
           "ORDER BY p.version DESC LIMIT 1")
    Optional<Port> findByPortCodeAndCodeSystem(@Param("portCode") String portCode, 
                                               @Param("codeSystem") CodeSystem codeSystem);
    
    @Query("SELECT p FROM Port p WHERE p.isActive = true " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE)")
    List<Port> findAllActive();
    
    @Query("SELECT p FROM Port p WHERE p.codeSystem = :codeSystem " +
           "AND (p.validTo IS NULL OR p.validTo > CURRENT_DATE)")
    Page<Port> findByCodeSystem(@Param("codeSystem") CodeSystem codeSystem, Pageable pageable);
    
    @Query("SELECT p FROM Port p WHERE " +
           "p.validFrom <= :date AND (p.validTo IS NULL OR p.validTo > :date)")
    List<Port> findValidAtDate(@Param("date") LocalDate date);
    
    @Query("SELECT p FROM Port p WHERE p.portCode = :portCode ORDER BY p.version")
    List<Port> findAllVersionsByPortCode(@Param("portCode") String portCode);
    
    @Query("SELECT p FROM Port p WHERE " +
           "LOWER(p.portName) LIKE LOWER(CONCAT('%', :pattern, '%'))")
    List<Port> findByPortNameContainingIgnoreCase(@Param("pattern") String pattern);
}