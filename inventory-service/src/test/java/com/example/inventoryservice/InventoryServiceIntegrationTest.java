package com.example.inventoryservice;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.model.Inventory;
import com.example.inventoryservice.repository.InventoryRepository;
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
class InventoryServiceIntegrationTest {

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

    @LocalServerPort
    private int port;

    @Autowired
    private InventoryRepository inventoryRepository;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        restTemplate = new RestTemplate();
    }

    @Test
    void testInventoryController_ShouldReturnInventory_WhenInventoryExists() {
        // Arrange
        Inventory inventory = new Inventory(1L, "TEST-SKU", 10);
        inventoryRepository.save(inventory);

        // Act
        String baseUrl = "http://localhost:" + port;
        ResponseEntity<InventoryResponse> response = restTemplate.getForEntity(
                baseUrl + "/api/inventory/TEST-SKU",
                InventoryResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TEST-SKU", response.getBody().skuCode());
        assertEquals(10, response.getBody().quantity());
    }

    @Test
    void testInventoryController_ShouldCreateInventory_WhenUpsertCalled() {
        // Arrange
        InventoryRequest request = new InventoryRequest("NEW-SKU", 25);

        // Act
        String baseUrl = "http://localhost:" + port;
        ResponseEntity<InventoryResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/inventory",
                request,
                InventoryResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NEW-SKU", response.getBody().skuCode());
        assertEquals(25, response.getBody().quantity());
    }
}
