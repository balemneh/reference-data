package gov.dhs.cbp.reference.loaders.iso.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * H2 test configuration for ISO loader module.
 * Replaces Testcontainers PostgreSQL with H2 in PostgreSQL compatibility mode.
 */
@TestConfiguration
public class TestContainersConfig {

    /**
     * H2 DataSource configured in PostgreSQL compatibility mode.
     * Schema creation is handled by the INIT parameter in the URL.
     */
    @Bean
    @Primary
    public DataSource h2TestDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        // Use PostgreSQL compatibility mode with appropriate settings and schema initialization
        dataSource.setUrl("jdbc:h2:mem:isoTestDb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;CASE_INSENSITIVE_IDENTIFIERS=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS reference_data");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}