package com.example.order_service;

import com.example.order_service.client.InventoryClient; // Проверь импорт!
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.external.dto.InventoryRequest;
import com.example.order_service.external.dto.InventoryResponse;
import com.example.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"order-placed"}, partitions = 1)
@ActiveProfiles("test")
@Testcontainers
class OrderServiceIntegrationTest {

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

    @LocalServerPort
    private int port;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private InventoryClient inventoryClient;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        restTemplate = new RestTemplate();
    }

    @Test
    void testOrderController_ShouldCreateOrder_WhenProductInStock() {
        // Arrange
        OrderRequest.UserDetails details = new OrderRequest.UserDetails("Ivan", "Ivanov", "ivan@mail.com");
        OrderRequest request = new OrderRequest("TEST-SKU", 10, details);

        when(inventoryClient.isInStock(anyString(), anyInt())).thenReturn(true);
        when(inventoryClient.decreaseInventory(any(InventoryRequest.class)))
                .thenReturn(ResponseEntity.ok(new InventoryResponse(1L, "TEST-SKU", 10)));

        // Act
        String baseUrl = "http://localhost:" + port + "/api/order";
        ResponseEntity<Boolean> response = restTemplate.postForEntity(baseUrl, request, Boolean.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody());
    }

    @Test
    void testOrderController_ShouldReturnFalse_WhenProductOutOfStock() {
        // Arrange
        OrderRequest.UserDetails details = new OrderRequest.UserDetails("Ivan", "Ivanov", "ivan@mail.com");
        OrderRequest request = new OrderRequest("TEST-SKU", 10, details);

        when(inventoryClient.isInStock(anyString(), anyInt())).thenReturn(false);

        // Act
        String baseUrl = "http://localhost:" + port + "/api/order";
        ResponseEntity<Boolean> response = restTemplate.postForEntity(baseUrl, request, Boolean.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody());
    }
}