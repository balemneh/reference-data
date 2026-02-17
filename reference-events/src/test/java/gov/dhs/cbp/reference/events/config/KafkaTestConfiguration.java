package gov.dhs.cbp.reference.events.config;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Kafka test configuration placeholder.
 * The actual embedded Kafka is configured via @EmbeddedKafka annotation on test classes.
 */
@TestConfiguration
public class KafkaTestConfiguration {
    // Configuration is handled by @EmbeddedKafka annotation in test classes
    // This class exists to satisfy the import in ChangeRequestEventPublisherIntegrationTest
}