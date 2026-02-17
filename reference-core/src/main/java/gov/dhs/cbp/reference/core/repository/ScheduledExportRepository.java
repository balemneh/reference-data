package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.ScheduledExport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScheduledExportRepository extends JpaRepository<ScheduledExport, UUID> {
}
