package gov.dhs.cbp.reference.core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Test-specific entity configuration that removes schema prefixes for H2 compatibility.
 * This configuration ensures that entities work with H2 database in tests
 * without requiring schema prefixes.
 */
@TestConfiguration
@Profile("integration-test")
public class TestEntityConfiguration {

    /**
     * Custom naming strategy that ignores schema annotations for H2 tests.
     */
    @Bean
    @Primary
    public PhysicalNamingStrategy testPhysicalNamingStrategy() {
        return new PhysicalNamingStrategy() {
            @Override
            public Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
                // Always return null for schema in test mode to avoid schema prefixing
                return null;
            }

            @Override
            public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
                return logicalName;
            }

            @Override
            public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
                return logicalName;
            }

            @Override
            public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
                return logicalName;
            }

            @Override
            public Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
                return logicalName;
            }
        };
    }
}