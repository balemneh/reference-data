package gov.dhs.cbp.reference.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "gov.dhs.cbp.reference.api",
    "gov.dhs.cbp.reference.core"
})
@EntityScan(basePackages = "gov.dhs.cbp.reference.core.entity")
@EnableJpaRepositories(basePackages = "gov.dhs.cbp.reference.core.repository")
public class ReferenceApiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ReferenceApiApplication.class, args);
    }
}