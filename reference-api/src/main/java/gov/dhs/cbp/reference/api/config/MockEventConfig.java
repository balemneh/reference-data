package gov.dhs.cbp.reference.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class MockEventConfig {

    @Bean(name = "outboxPublisher")
    @ConditionalOnMissingBean(name = "outboxPublisher")
    public Object mockOutboxPublisher() {
        // Return null for now - the service should handle null publisher
        return null;
    }
}