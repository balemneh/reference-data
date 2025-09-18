package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.Airport;
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
public interface AirportRepository extends BitemporalRepository<Airport> {
    
    @Query("SELECT a FROM Airport a WHERE a.iataCode = :iataCode AND a.codeSystem.id = :systemId " +
           "AND (a.validTo IS NULL OR a.validTo > CURRENT_DATE) " +
           "ORDER BY a.version DESC")
    Optional<Airport> findCurrentByIataCodeAndSystem(@Param("iataCode") String iataCode, 
                                                     @Param("systemId") UUID systemId);
    
    @Query("SELECT a FROM Airport a WHERE a.iataCode = :iataCode AND a.codeSystem.code = :systemCode " +
           "AND (a.validTo IS NULL OR a.validTo > CURRENT_DATE) " +
           "ORDER BY a.version DESC")
    Optional<Airport> findCurrentByIataCodeAndSystemCode(@Param("iataCode") String iataCode, 
                                                         @Param("systemCode") String systemCode);
    
    @Query("SELECT a FROM Airport a WHERE a.icaoCode = :icaoCode AND a.codeSystem.code = :systemCode " +
           "AND (a.validTo IS NULL OR a.validTo > CURRENT_DATE) " +
           "ORDER BY a.version DESC")
    Optional<Airport> findCurrentByIcaoCodeAndSystemCode(@Param("icaoCode") String icaoCode, 
                                                         @Param("systemCode") String systemCode);
    
    @Query("SELECT a FROM Airport a WHERE a.codeSystem.code = :systemCode " +
           "AND (a.validTo IS NULL OR a.validTo > CURRENT_DATE) " +
           "ORDER BY a.airportName")
    Page<Airport> findCurrentBySystemCode(@Param("systemCode") String systemCode, Pageable pageable);
    
    @Query("SELECT a FROM Airport a WHERE a.countryCode = :countryCode " +
           "AND (a.validTo IS NULL OR a.validTo > CURRENT_DATE)")
    List<Airport> findCurrentByCountryCode(@Param("countryCode") String countryCode);
    
    @Query("SELECT a FROM Airport a WHERE a.city = :city " +
           "AND (a.validTo IS NULL OR a.validTo > CURRENT_DATE)")
    List<Airport> findCurrentByCity(@Param("city") String city);
    
    @Query("SELECT a FROM Airport a WHERE a.airportType = :airportType " +
           "AND (a.validTo IS NULL OR a.validTo > CURRENT_DATE)")
    List<Airport> findCurrentByAirportType(@Param("airportType") String airportType);
    
    @Query("SELECT a FROM Airport a WHERE a.iataCode = :iataCode " +
           "AND a.codeSystem.code = :systemCode " +
           "AND a.validFrom <= :asOfDate " +
           "AND (a.validTo IS NULL OR a.validTo > :asOfDate) " +
           "ORDER BY a.version DESC")
    Optional<Airport> findByIataCodeAndSystemAsOf(@Param("iataCode") String iataCode,
                                                  @Param("systemCode") String systemCode,
                                                  @Param("asOfDate") LocalDate asOfDate);
    
    @Query("SELECT a FROM Airport a WHERE a.icaoCode = :icaoCode " +
           "AND a.codeSystem.code = :systemCode " +
           "AND a.validFrom <= :asOfDate " +
           "AND (a.validTo IS NULL OR a.validTo > :asOfDate) " +
           "ORDER BY a.version DESC")
    Optional<Airport> findByIcaoCodeAndSystemAsOf(@Param("icaoCode") String icaoCode,
                                                  @Param("systemCode") String systemCode,
                                                  @Param("asOfDate") LocalDate asOfDate);
    
    @Query("SELECT a FROM Airport a WHERE " +
           "LOWER(a.airportName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND (a.validTo IS NULL OR a.validTo > CURRENT_DATE)")
    Page<Airport> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT DISTINCT a.iataCode FROM Airport a WHERE a.codeSystem.code = :systemCode " +
           "AND a.iataCode IS NOT NULL " +
           "ORDER BY a.iataCode")
    List<String> findAllIataCodes(@Param("systemCode") String systemCode);
    
    @Query("SELECT DISTINCT a.icaoCode FROM Airport a WHERE a.codeSystem.code = :systemCode " +
           "AND a.icaoCode IS NOT NULL " +
           "ORDER BY a.icaoCode")
    List<String> findAllIcaoCodes(@Param("systemCode") String systemCode);
    
    @Query("SELECT COUNT(a) FROM Airport a WHERE a.isActive = true " +
           "AND (a.validTo IS NULL OR a.validTo > CURRENT_DATE)")
    long countByIsActiveTrue();
    
    @Query("SELECT a FROM Airport a ORDER BY a.recordedAt DESC LIMIT 1")
    Optional<Airport> findTopByOrderByRecordedAtDesc();
    
    // Additional methods for integration tests
    
    @Query("SELECT a FROM Airport a WHERE a.iataCode = :iataCode AND a.codeSystem = :codeSystem " +
           "AND (a.validTo IS NULL OR a.validTo > CURRENT_DATE) " +
           "ORDER BY a.version DESC LIMIT 1")
    Optional<Airport> findByIataCodeAndCodeSystem(@Param("iataCode") String iataCode, 
                                                  @Param("codeSystem") CodeSystem codeSystem);
    
    @Query("SELECT a FROM Airport a WHERE a.isActive = true " +
           "AND (a.validTo IS NULL OR a.validTo > CURRENT_DATE)")
    List<Airport> findAllActive();
    
    @Query("SELECT a FROM Airport a WHERE a.codeSystem = :codeSystem " +
           "AND (a.validTo IS NULL OR a.validTo > CURRENT_DATE)")
    Page<Airport> findByCodeSystem(@Param("codeSystem") CodeSystem codeSystem, Pageable pageable);
    
    @Query("SELECT a FROM Airport a WHERE " +
           "a.validFrom <= :date AND (a.validTo IS NULL OR a.validTo > :date)")
    List<Airport> findValidAtDate(@Param("date") LocalDate date);
    
    @Query("SELECT a FROM Airport a WHERE a.iataCode = :iataCode ORDER BY a.version")
    List<Airport> findAllVersionsByIataCode(@Param("iataCode") String iataCode);
    
    @Query("SELECT a FROM Airport a WHERE " +
           "LOWER(a.airportName) LIKE LOWER(CONCAT('%', :pattern, '%'))")
    List<Airport> findByAirportNameContainingIgnoreCase(@Param("pattern") String pattern);
}