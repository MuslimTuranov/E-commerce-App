package com.example.order_service;

import com.example.order_service.client.InventoryClient;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.event.OrderPlacedEvent;
import com.example.order_service.external.dto.InventoryRequest;
import com.example.order_service.external.dto.InventoryResponse;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.service.OrderService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"order-placed"}, partitions = 1)
@Testcontainers
@DirtiesContext
@ActiveProfiles("test")
class OrderServiceKafkaTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("order-db")
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
    private OrderRepository orderRepository;

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    private Consumer<String, OrderPlacedEvent> consumer;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "testGroup");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new org.apache.kafka.common.serialization.StringDeserializer(),
                new JsonDeserializer<>(OrderPlacedEvent.class)
        ).createConsumer();

        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "order-placed");
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void testKafkaIntegration_ShouldSendOrderPlacedEvent_WhenOrderCreated() {
        // Arrange
        OrderRequest.UserDetails details = new OrderRequest.UserDetails("Ivan", "Ivanov", "ivan@mail.com");
        OrderRequest request = new OrderRequest("TEST-SKU", 10, details);

        // Mock inventory client to return in stock
        InventoryClient inventoryClient = new InventoryClient() {
            @Override
            public boolean isInStock(String skuCode, Integer quantity) {
                return true;
            }

            @Override
            public ResponseEntity<InventoryResponse> decreaseInventory(InventoryRequest request) {
                return ResponseEntity.ok(new InventoryResponse(1L, request.skuCode(), request.quantity()));
            }

            public ResponseEntity<InventoryResponse> getInventoryBySkuCode(String skuCode) {
                return ResponseEntity.ok(new InventoryResponse(1L, skuCode, 10));
            }
        };

        // Create order service with mocked inventory client
        OrderService orderService = new OrderService(orderRepository, inventoryClient, kafkaTemplate);

        // Act
        boolean result = orderService.placeOrder(request);

        // Wait for Kafka message
        ConsumerRecords<String, OrderPlacedEvent> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

        // Assert
        assertTrue(result);
        assertTrue(records.count() > 0);
        boolean orderPlacedFound = false;
        for (ConsumerRecord<String, OrderPlacedEvent> record : records) {
            if (record.topic().equals("order-placed")) {
                orderPlacedFound = true;
                OrderPlacedEvent event = record.value();
                assertNotNull(event.getOrderNumber());
                break;
            }
        }
        assertTrue(orderPlacedFound, "Order placed event should be sent");
    }

    // Simple deserializers for testing
    static class StringDeserializer implements org.apache.kafka.common.serialization.Deserializer<String> {
        @Override
        public String deserialize(String topic, byte[] data) {
            return new String(data);
        }
    }

    static class JsonDeserializer<T> implements org.apache.kafka.common.serialization.Deserializer<T> {
        private final Class<T> type;

        public JsonDeserializer(Class<T> type) {
            this.type = type;
        }

        @Override
        public T deserialize(String topic, byte[] data) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(data, type);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize JSON", e);
            }
        }
    }
}
