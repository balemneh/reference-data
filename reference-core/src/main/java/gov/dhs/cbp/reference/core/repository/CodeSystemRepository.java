package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.CodeSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodeSystemRepository extends JpaRepository<CodeSystem, UUID> {
    
    Optional<CodeSystem> findByCode(String code);
    
    boolean existsByCode(String code);
}