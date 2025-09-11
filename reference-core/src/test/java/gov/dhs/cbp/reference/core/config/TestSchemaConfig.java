package gov.dhs.cbp.reference.core.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

@TestConfiguration
public class TestSchemaConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return (hibernateProperties) -> {
            // Override schema handling for H2 tests
            hibernateProperties.put("hibernate.default_schema", "");
            hibernateProperties.put("hibernate.physical_naming_strategy", TestPhysicalNamingStrategy.class.getName());
        };
    }

    public static class TestPhysicalNamingStrategy implements PhysicalNamingStrategy {
        @Override
        public Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment context) {
            return logicalName;
        }

        @Override
        public Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment context) {
            // Ignore schema for H2 tests
            return null;
        }

        @Override
        public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment context) {
            return logicalName;
        }

        @Override
        public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment context) {
            return logicalName;
        }

        @Override
        public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment context) {
            return logicalName;
        }
    }
}