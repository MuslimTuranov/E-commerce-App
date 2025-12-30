package com.example.inventoryservice;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.model.Inventory;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.service.InventoryService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"inventory-events"}, partitions = 1)
@Testcontainers
@DirtiesContext
@ActiveProfiles("test")
class InventoryServiceKafkaTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("inventory-db")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.admin.properties.offsets.topic.num.partitions", () -> "1");
        registry.add("spring.kafka.admin.properties.offsets.topic.replication.factor", () -> "1");
        registry.add("spring.kafka.bootstrap-servers", () ->
                System.getProperty("spring.embedded.kafka.brokers", "localhost:9092"));
    }

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryService inventoryService;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();

        Map<String, Object> props = KafkaTestUtils.consumerProps(
                embeddedKafka,
                "testGroup",
                true
        );

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new DefaultKafkaConsumerFactory<>(
                props,
                new org.apache.kafka.common.serialization.StringDeserializer(),
                new org.apache.kafka.common.serialization.StringDeserializer()
        ).createConsumer();

        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "inventory-events");
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void testKafkaIntegration_ShouldSendInventoryUpdatedEvent_WhenInventoryUpserted() {
        InventoryRequest request = new InventoryRequest("TEST-SKU", 10);

        InventoryResponse response = inventoryService.upsertInventory(request);

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));

        assertNotNull(records, "Records should not be null");
        assertTrue(records.count() > 0, "Should receive at least one record");

        boolean inventoryUpdatedFound = false;
        for (ConsumerRecord<String, String> record : records) {
            if (record.topic().equals("inventory-events")) {
                inventoryUpdatedFound = true;
                assertTrue(record.value().contains("TEST-SKU"));
                assertTrue(record.value().contains(String.valueOf(response.quantity())));
                break;
            }
        }
        assertTrue(inventoryUpdatedFound, "Inventory updated event should be sent to 'inventory-events' topic");
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return new InventoryResponse(inventory.getId(), inventory.getSkuCode(), inventory.getQuantity());
    }
}
