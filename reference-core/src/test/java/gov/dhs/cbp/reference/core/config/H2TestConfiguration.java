package gov.dhs.cbp.reference.core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Shared H2 test configuration for enterprise CI/CD compatibility.
 * Replaces Testcontainers PostgreSQL with H2 in PostgreSQL compatibility mode.
 * Schema initialization is handled by Hibernate DDL with create_namespaces enabled.
 */
@TestConfiguration
public class H2TestConfiguration {

    /**
     * H2 DataSource configured in PostgreSQL compatibility mode with schema creation.
     * The INIT parameter creates the reference_data schema on connection.
     * Hibernate DDL will then create tables within that schema.
     */
    @Bean
    @Primary
    public DataSource h2TestDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        // Use PostgreSQL compatibility mode with schema initialization
        dataSource.setUrl("jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;CASE_INSENSITIVE_IDENTIFIERS=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS reference_data");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}