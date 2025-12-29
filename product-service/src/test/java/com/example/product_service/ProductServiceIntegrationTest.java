package com.example.product_service;

import com.example.product_service.dto.ProductRequest;
import com.example.product_service.dto.ProductResponse;
import com.example.product_service.model.Product;
import com.example.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductServiceIntegrationTest {

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
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void testProductController_ShouldCreateProduct() {
        // Arrange
        ProductRequest request = new ProductRequest(
                "TEST-SKU",
                "Test Product",
                "Test Description",
                new BigDecimal("99.99"),
                10
        );

        // Act
        ResponseEntity<ProductResponse> response = restTemplate.postForEntity(
                "/api/products",
                request,
                ProductResponse.class
        );

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TEST-SKU", response.getBody().skuCode());
        assertEquals("Test Product", response.getBody().name());
        assertEquals(99.99, response.getBody().price());
    }

    @Test
    void testProductController_ShouldGetAllProducts() {
        // Arrange
        Product product1 = new Product();
        product1.setId(1L);
        product1.setSkuCode("SKU-1");
        product1.setName("Product 1");
        product1.setDescription("Desc 1");
        product1.setPrice(new BigDecimal("10.0"));
        product1.setQuantity(5);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setSkuCode("SKU-2");
        product2.setName("Product 2");
        product2.setDescription("Desc 2");
        product2.setPrice(new BigDecimal("20.0"));
        product2.setQuantity(10);
        productRepository.saveAll(List.of(product1, product2));

        // Act
        ResponseEntity<ProductResponse[]> response = restTemplate.getForEntity(
                "/api/products",
                ProductResponse[].class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().length);
    }
}
