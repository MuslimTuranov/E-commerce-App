package com.example.inventoryservice;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.model.Inventory;
import com.example.inventoryservice.repository.InventoryRepository;
import org.apache.kafka.clients.consumer.Consumer;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"inventory-updated"}, partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@Testcontainers
@DirtiesContext
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
    }

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private InventoryRepository inventoryRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka);
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "inventory-updated");
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void testKafkaIntegration_ShouldSendInventoryUpdatedEvent_WhenInventoryUpserted() {
        // Arrange
        InventoryRequest request = new InventoryRequest("TEST-SKU", 10);

        // Act
        InventoryResponse response = inventoryRepository.findBySkuCode("TEST-SKU")
                .map(inventory -> {
                    inventory.setQuantity(inventory.getQuantity() + request.quantity());
                    return mapToResponse(inventoryRepository.save(inventory));
                })
                .orElseGet(() -> {
                    Inventory newInventory = new Inventory();
                    newInventory.setSkuCode(request.skuCode());
                    newInventory.setQuantity(request.quantity());
                    return mapToResponse(inventoryRepository.save(newInventory));
                });

        // Wait for Kafka message
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

        // Assert
        assertTrue(records.count() > 0);
        boolean inventoryUpdatedFound = false;
        for (ConsumerRecord<String, String> record : records) {
            if (record.topic().equals("inventory-updated")) {
                inventoryUpdatedFound = true;
                assertTrue(record.value().contains("TEST-SKU"));
                assertTrue(record.value().contains(String.valueOf(response.quantity())));
                break;
            }
        }
        assertTrue(inventoryUpdatedFound, "Inventory updated event should be sent");
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return new InventoryResponse(inventory.getId(), inventory.getSkuCode(), inventory.getQuantity());
    }
}
