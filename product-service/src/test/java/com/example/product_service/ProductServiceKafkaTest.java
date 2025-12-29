package com.example.product_service;

import com.example.product_service.dto.ProductRequest;
import com.example.product_service.dto.ProductResponse;
import com.example.product_service.external.client.InventoryClient;
import com.example.product_service.external.dto.InventoryResponse;
import com.example.product_service.model.Product;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.service.ProductService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"product-created"}, partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@Testcontainers
@DirtiesContext
class ProductServiceKafkaTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("product-db")
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
    private ProductRepository productRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka);
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "product-created");
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void testKafkaIntegration_ShouldSendProductCreatedEvent_WhenProductCreated() {
        // Arrange
        ProductRequest request = new ProductRequest(
                "NEW-SKU",
                "New Product",
                "New Description",
                new BigDecimal("99.99"),
                10
        );

        // Mock inventory client
        InventoryClient inventoryClient = new InventoryClient() {
            @Override
            public ResponseEntity<InventoryResponse> getInventoryBySkuCode(String skuCode) {
                return ResponseEntity.ok(new InventoryResponse(1L, skuCode, 10));
            }
        };

        // Create product service with mocked inventory client
        ProductService productService = new ProductService(productRepository, null, inventoryClient);

        // Act
        ProductResponse response = productService.createProduct(request);

        // Wait for Kafka message
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

        // Assert
        assertNotNull(response);
        assertEquals("NEW-SKU", response.skuCode());
        assertTrue(records.count() > 0);
        boolean productCreatedFound = false;
        for (ConsumerRecord<String, String> record : records) {
            if (record.topic().equals("product-created")) {
                productCreatedFound = true;
                assertTrue(record.value().contains("NEW-SKU"));
                assertTrue(record.value().contains("10"));
                break;
            }
        }
        assertTrue(productCreatedFound, "Product created event should be sent");
    }
}
