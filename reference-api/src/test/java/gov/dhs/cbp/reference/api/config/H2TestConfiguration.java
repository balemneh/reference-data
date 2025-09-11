package gov.dhs.cbp.reference.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * H2 database configuration for integration tests.
 * The H2 database configuration is provided via application-integration-test.yml
 */
@TestConfiguration
@Profile("integration-test")
public class H2TestConfiguration {
    // Configuration is handled by Spring Boot autoconfiguration
    // using properties from application-integration-test.yml
}