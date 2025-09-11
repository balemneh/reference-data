package gov.dhs.cbp.reference.events.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KafkaConfigTest {

    private KafkaConfig kafkaConfig;

    @BeforeEach
    void setUp() {
        kafkaConfig = new KafkaConfig();
        ReflectionTestUtils.setField(kafkaConfig, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(kafkaConfig, "schemaRegistryUrl", "http://localhost:8081");
    }

    @Test
    void testKafkaAdminCreation() {
        KafkaAdmin kafkaAdmin = kafkaConfig.kafkaAdmin();
        
        assertNotNull(kafkaAdmin);
        assertTrue(kafkaAdmin.getConfigurationProperties().containsKey("bootstrap.servers"));
        assertEquals("localhost:9092", kafkaAdmin.getConfigurationProperties().get("bootstrap.servers"));
    }

    @Test
    void testCountriesTopicCreation() {
        NewTopic topic = kafkaConfig.countriesTopic();
        
        assertNotNull(topic);
        assertEquals("reference-data.countries", topic.name());
        assertEquals(3, topic.numPartitions());
        assertEquals(1, topic.replicationFactor());
        assertEquals("604800000", topic.configs().get("retention.ms"));
        assertEquals("snappy", topic.configs().get("compression.type"));
    }

    @Test
    void testPortsTopicCreation() {
        NewTopic topic = kafkaConfig.portsTopic();
        
        assertNotNull(topic);
        assertEquals("reference-data.ports", topic.name());
        assertEquals(3, topic.numPartitions());
        assertEquals(1, topic.replicationFactor());
    }

    @Test
    void testAirportsTopicCreation() {
        NewTopic topic = kafkaConfig.airportsTopic();
        
        assertNotNull(topic);
        assertEquals("reference-data.airports", topic.name());
        assertEquals(3, topic.numPartitions());
        assertEquals(1, topic.replicationFactor());
    }

    @Test
    void testCarriersTopicCreation() {
        NewTopic topic = kafkaConfig.carriersTopic();
        
        assertNotNull(topic);
        assertEquals("reference-data.carriers", topic.name());
        assertEquals(3, topic.numPartitions());
        assertEquals(1, topic.replicationFactor());
    }

    @Test
    void testMappingsTopicCreation() {
        NewTopic topic = kafkaConfig.mappingsTopic();
        
        assertNotNull(topic);
        assertEquals("reference-data.mappings", topic.name());
        assertEquals(3, topic.numPartitions());
        assertEquals(1, topic.replicationFactor());
    }

    @Test
    void testDeadLetterTopicCreation() {
        NewTopic topic = kafkaConfig.deadLetterTopic();
        
        assertNotNull(topic);
        assertEquals("reference-data.dead-letter", topic.name());
        assertEquals(1, topic.numPartitions());
        assertEquals(1, topic.replicationFactor());
        assertEquals("2592000000", topic.configs().get("retention.ms"));
    }

    @Test
    void testProducerFactoryCreation() {
        ProducerFactory<String, Object> producerFactory = kafkaConfig.producerFactory();
        
        assertNotNull(producerFactory);
        var configs = producerFactory.getConfigurationProperties();
        assertEquals("localhost:9092", configs.get("bootstrap.servers"));
        assertEquals("all", configs.get("acks"));
        assertEquals(3, configs.get("retries"));
        assertEquals(true, configs.get("enable.idempotence"));
    }

    @Test
    void testAvroProducerFactoryCreation() {
        ProducerFactory<String, Object> avroProducerFactory = kafkaConfig.avroProducerFactory();
        
        assertNotNull(avroProducerFactory);
        var configs = avroProducerFactory.getConfigurationProperties();
        assertEquals("localhost:9092", configs.get("bootstrap.servers"));
        assertEquals("http://localhost:8081", configs.get("schema.registry.url"));
        assertEquals("all", configs.get("acks"));
        assertEquals(true, configs.get("enable.idempotence"));
    }

    @Test
    void testKafkaTemplateCreation() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaConfig.kafkaTemplate();
        
        assertNotNull(kafkaTemplate);
        assertNotNull(kafkaTemplate.getProducerFactory());
    }

    @Test
    void testAvroKafkaTemplateCreation() {
        KafkaTemplate<String, Object> avroKafkaTemplate = kafkaConfig.avroKafkaTemplate();
        
        assertNotNull(avroKafkaTemplate);
        assertNotNull(avroKafkaTemplate.getProducerFactory());
    }

    @Test
    void testTopicRetentionConfiguration() {
        NewTopic countriesTopic = kafkaConfig.countriesTopic();
        NewTopic deadLetterTopic = kafkaConfig.deadLetterTopic();
        
        // 7 days for regular topics
        assertEquals("604800000", countriesTopic.configs().get("retention.ms"));
        
        // 30 days for dead letter topic
        assertEquals("2592000000", deadLetterTopic.configs().get("retention.ms"));
    }

    @Test
    void testProducerIdempotenceConfiguration() {
        ProducerFactory<String, Object> producerFactory = kafkaConfig.producerFactory();
        ProducerFactory<String, Object> avroProducerFactory = kafkaConfig.avroProducerFactory();
        
        // Both producers should have idempotence enabled
        assertEquals(true, producerFactory.getConfigurationProperties().get("enable.idempotence"));
        assertEquals(true, avroProducerFactory.getConfigurationProperties().get("enable.idempotence"));
    }

    @Test
    void testProducerPerformanceConfiguration() {
        ProducerFactory<String, Object> producerFactory = kafkaConfig.producerFactory();
        var configs = producerFactory.getConfigurationProperties();
        
        // Batch and buffer settings
        assertEquals(16384, configs.get("batch.size"));
        assertEquals(10, configs.get("linger.ms"));
        assertEquals(33554432, configs.get("buffer.memory"));
        assertEquals(5, configs.get("max.in.flight.requests.per.connection"));
    }
}