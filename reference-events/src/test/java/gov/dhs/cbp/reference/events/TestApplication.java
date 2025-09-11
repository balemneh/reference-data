package gov.dhs.cbp.reference.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test application configuration for reference-events module tests.
 * This provides the Spring Boot configuration needed for integration tests.
 */
@SpringBootApplication(scanBasePackages = {
    "gov.dhs.cbp.reference.events",
    "gov.dhs.cbp.reference.core"
})
@EntityScan(basePackages = "gov.dhs.cbp.reference.core.entity")
@EnableJpaRepositories(basePackages = {
    "gov.dhs.cbp.reference.core.repository",
    "gov.dhs.cbp.reference.events.repository"
})
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}