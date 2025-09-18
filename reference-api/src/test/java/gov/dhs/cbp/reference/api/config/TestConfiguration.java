package gov.dhs.cbp.reference.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * Test configuration for comprehensive test suite.
 * Provides optimized test environment setup for different test scenarios.
 */
@TestConfiguration
public class TestConfiguration {

    /**
     * H2 in-memory database configuration for fast unit tests.
     */
    @Bean
    @Primary
    @Profile("test")
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb")
                .addScript("classpath:schema-h2.sql")
                .build();
    }

    /**
     * Performance test configuration with optimized settings.
     */
    @Bean
    @Profile("performance")
    public DataSource performanceDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("performancedb")
                .addScript("classpath:schema-h2.sql")
                .build();
    }
}