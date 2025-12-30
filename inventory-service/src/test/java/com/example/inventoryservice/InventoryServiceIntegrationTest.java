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
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@EmbeddedKafka(topics = {"inventory-events"}, partitions = 1)
@ActiveProfiles("test")
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
        registry.add("spring.kafka.bootstrap-servers", () ->
                System.getProperty("spring.embedded.kafka.brokers"));
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
        Inventory inventory = new Inventory();
        inventory.setSkuCode("TEST-SKU");
        inventory.setQuantity(10);
        inventoryRepository.save(inventory);

        String url = "http://localhost:" + port + "/api/inventory/skuCode?skuCode=TEST-SKU";
        ResponseEntity<InventoryResponse> response = restTemplate.getForEntity(url, InventoryResponse.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TEST-SKU", response.getBody().skuCode());
    }

    @Test
    void testInventoryController_ShouldCreateInventory_WhenUpsertCalled() {
        // Arrange
        InventoryRequest request = new InventoryRequest("NEW-SKU", 25);
        String url = "http://localhost:" + port + "/api/inventory/updateQuantity";

        ResponseEntity<InventoryResponse> response = restTemplate.postForEntity(
                url,
                request,
                InventoryResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NEW-SKU", response.getBody().skuCode());
    }
}
