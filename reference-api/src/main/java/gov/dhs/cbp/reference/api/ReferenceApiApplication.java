package gov.dhs.cbp.reference.api;

import gov.dhs.cbp.reference.api.config.KeycloakAdminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import gov.dhs.cbp.reference.api.JwtDecoderConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = {
    "gov.dhs.cbp.reference.api",
    "gov.dhs.cbp.reference.core",
    "gov.dhs.cbp.reference.events"
})
@EntityScan(basePackages = "gov.dhs.cbp.reference.core.entity")
@EnableJpaRepositories(basePackages = "gov.dhs.cbp.reference.core.repository")
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class ReferenceApiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ReferenceApiApplication.class, args);
    }
}