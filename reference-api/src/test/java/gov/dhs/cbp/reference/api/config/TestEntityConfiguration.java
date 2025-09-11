package gov.dhs.cbp.reference.api.config;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration to handle entity naming strategy for H2 database.
 * This configuration removes schema prefixes for H2 compatibility while
 * maintaining table and column naming conventions.
 */
@TestConfiguration
public class TestEntityConfiguration {

    @Bean
    public org.hibernate.boot.model.naming.PhysicalNamingStrategy physicalNamingStrategy() {
        return new CamelCaseToUnderscoresNamingStrategy() {
            @Override
            public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
                // Return null to ignore schema annotations in test mode
                return null;
            }
        };
    }
}