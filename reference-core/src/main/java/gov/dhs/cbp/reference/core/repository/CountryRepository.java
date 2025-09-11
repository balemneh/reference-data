package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.entity.Country;
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
public interface CountryRepository extends BitemporalRepository<Country> {
    
    @Query("SELECT c FROM Country c WHERE c.countryCode = :code AND c.codeSystem.id = :systemId " +
           "AND (c.validTo IS NULL OR c.validTo > CURRENT_DATE) " +
           "ORDER BY c.version DESC")
    Optional<Country> findCurrentByCodeAndSystem(@Param("code") String code, 
                                                 @Param("systemId") UUID systemId);
    
    @Query("SELECT c FROM Country c WHERE c.countryCode = :code AND c.codeSystem.code = :systemCode " +
           "AND (c.validTo IS NULL OR c.validTo > CURRENT_DATE) " +
           "ORDER BY c.version DESC")
    Optional<Country> findCurrentByCodeAndSystemCode(@Param("code") String code, 
                                                     @Param("systemCode") String systemCode);
    
    @Query("SELECT c FROM Country c WHERE c.codeSystem.code = :systemCode " +
           "AND (c.validTo IS NULL OR c.validTo > CURRENT_DATE) " +
           "ORDER BY c.countryCode")
    Page<Country> findCurrentBySystemCode(@Param("systemCode") String systemCode, Pageable pageable);
    
    @Query("SELECT c FROM Country c WHERE c.iso2Code = :iso2 " +
           "AND (c.validTo IS NULL OR c.validTo > CURRENT_DATE)")
    List<Country> findCurrentByIso2Code(@Param("iso2") String iso2);
    
    @Query("SELECT c FROM Country c WHERE c.iso3Code = :iso3 " +
           "AND (c.validTo IS NULL OR c.validTo > CURRENT_DATE)")
    List<Country> findCurrentByIso3Code(@Param("iso3") String iso3);
    
    @Query("SELECT c FROM Country c WHERE c.countryCode = :code " +
           "AND c.codeSystem.code = :systemCode " +
           "AND c.validFrom <= :asOfDate " +
           "AND (c.validTo IS NULL OR c.validTo > :asOfDate) " +
           "ORDER BY c.version DESC")
    Optional<Country> findByCodeAndSystemAsOf(@Param("code") String code,
                                              @Param("systemCode") String systemCode,
                                              @Param("asOfDate") LocalDate asOfDate);
    
    @Query("SELECT c FROM Country c WHERE " +
           "LOWER(c.countryName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND (c.validTo IS NULL OR c.validTo > CURRENT_DATE)")
    Page<Country> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT DISTINCT c.countryCode FROM Country c WHERE c.codeSystem.code = :systemCode " +
           "ORDER BY c.countryCode")
    List<String> findAllCountryCodes(@Param("systemCode") String systemCode);
    
    @Query("SELECT COUNT(c) FROM Country c WHERE c.isActive = true " +
           "AND (c.validTo IS NULL OR c.validTo > CURRENT_DATE)")
    long countByIsActiveTrue();
    
    @Query("SELECT c FROM Country c ORDER BY c.recordedAt DESC LIMIT 1")
    Optional<Country> findTopByOrderByRecordedAtDesc();
    
    // Alias method for compatibility
    default Optional<Country> findByCountryCodeAndSystem(String code, String systemCode) {
        return findCurrentByCodeAndSystemCode(code, systemCode);
    }
    
    default Optional<Country> findByCountryCodeAndSystemCode(String code, String systemCode) {
        return findCurrentByCodeAndSystemCode(code, systemCode);
    }
    
    // Additional methods for integration tests
    
    @Query("SELECT c FROM Country c WHERE c.countryCode = :code AND c.codeSystem = :codeSystem " +
           "AND (c.validTo IS NULL OR c.validTo > CURRENT_DATE) " +
           "ORDER BY c.version DESC LIMIT 1")
    Optional<Country> findByCountryCodeAndCodeSystem(@Param("code") String code, 
                                                    @Param("codeSystem") CodeSystem codeSystem);
    
    @Query("SELECT c FROM Country c WHERE c.isActive = true " +
           "AND (c.validTo IS NULL OR c.validTo > CURRENT_DATE)")
    List<Country> findAllActive();
    
    @Query("SELECT c FROM Country c WHERE c.codeSystem = :codeSystem " +
           "AND (c.validTo IS NULL OR c.validTo > CURRENT_DATE)")
    Page<Country> findByCodeSystem(@Param("codeSystem") CodeSystem codeSystem, Pageable pageable);
    
    @Query("SELECT c FROM Country c WHERE " +
           "c.validFrom <= :date AND (c.validTo IS NULL OR c.validTo > :date)")
    List<Country> findValidAtDate(@Param("date") LocalDate date);
    
    @Query("SELECT c FROM Country c WHERE c.countryCode = :code ORDER BY c.version")
    List<Country> findAllVersionsByCountryCode(@Param("code") String code);
    
    @Query("SELECT c FROM Country c WHERE " +
           "LOWER(c.countryName) LIKE LOWER(CONCAT('%', :pattern, '%'))")
    List<Country> findByCountryNameContainingIgnoreCase(@Param("pattern") String pattern);
}