package gov.dhs.cbp.reference.loaders.iso.repository;

import gov.dhs.cbp.reference.loaders.iso.entity.IsoCountryStaging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IsoCountryStagingRepository extends JpaRepository<IsoCountryStaging, Long> {
    
    Optional<IsoCountryStaging> findByAlpha2Code(String alpha2Code);
    
    Optional<IsoCountryStaging> findByAlpha3Code(String alpha3Code);
    
    List<IsoCountryStaging> findByLoadExecutionId(String executionId);
    
    @Query("SELECT s FROM IsoCountryStaging s WHERE s.processingStatus = 'PENDING'")
    List<IsoCountryStaging> findPendingRecords();
    
    @Query("SELECT s FROM IsoCountryStaging s WHERE s.validationStatus = 'INVALID'")
    List<IsoCountryStaging> findInvalidRecords();
    
    void deleteByLoadExecutionId(String executionId);
}