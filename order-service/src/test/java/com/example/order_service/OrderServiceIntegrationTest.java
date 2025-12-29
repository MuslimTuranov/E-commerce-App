package com.example.order_service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        restTemplate = new RestTemplate();
    }

    @Test
    void testOrderController_ShouldCreateOrder_WhenProductInStock() {
        // Arrange
        OrderRequest request = new OrderRequest("TEST-SKU", 5);

        // Act
        String baseUrl = "http://localhost:" + port;
        ResponseEntity<Boolean> response = restTemplate.postForEntity(
                baseUrl + "/api/orders",
                request,
                Boolean.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
    }

    @Test
    void testOrderController_ShouldReturnFalse_WhenProductOutOfStock() {
        // Arrange
        OrderRequest request = new OrderRequest("OUT-OF-STOCK-SKU", 100);

        // Act
        String baseUrl = "http://localhost:" + port;
        ResponseEntity<Boolean> response = restTemplate.postForEntity(
                baseUrl + "/api/orders",
                request,
                Boolean.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody());
    }
}
