package gov.dhs.cbp.reference.core.repository;

import gov.dhs.cbp.reference.core.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SystemConfiguration entities.
 */
@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, UUID> {

    /**
     * Find configuration by key.
     */
    Optional<SystemConfiguration> findByConfigKey(String configKey);

    /**
     * Find all active configurations.
     */
    List<SystemConfiguration> findByIsActiveTrue();

    /**
     * Find configurations by type.
     */
    List<SystemConfiguration> findByConfigType(SystemConfiguration.ConfigurationType configType);

    /**
     * Find active configurations by type.
     */
    List<SystemConfiguration> findByConfigTypeAndIsActiveTrue(SystemConfiguration.ConfigurationType configType);

    /**
     * Check if a configuration key exists.
     */
    boolean existsByConfigKey(String configKey);

    /**
     * Get all configuration keys.
     */
    @Query("SELECT sc.configKey FROM SystemConfiguration sc WHERE sc.isActive = true")
    List<String> findAllActiveConfigKeys();
}