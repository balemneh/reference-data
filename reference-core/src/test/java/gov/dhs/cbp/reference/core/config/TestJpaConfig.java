package gov.dhs.cbp.reference.core.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Custom naming strategy for H2 tests that removes schema references
 * since H2 doesn't support the same schema structure as PostgreSQL.
 */
public class TestJpaConfig extends PhysicalNamingStrategyStandardImpl {

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment context) {
        // For H2 tests, ignore schema names
        return null;
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        // Keep table names as-is
        return name;
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        // Keep column names as-is
        return name;
    }
}