package com.example.product_service.service;

import com.example.product_service.dto.ProductRequest;
import com.example.product_service.dto.ProductResponse;
import com.example.product_service.external.client.InventoryClient;
import com.example.product_service.external.dto.InventoryResponse;
import com.example.product_service.kafka.ProductEventProducer;
import com.example.product_service.model.Product;
import com.example.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;


import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ProductServiceUnitTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductEventProducer producer;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private ProductService productService;

    private ProductRequest productRequest;
    private Product product;

    @BeforeEach
    void setUp() {
        productRequest = new ProductRequest(
                "TEST-SKU",
                "Test Product",
                "Test Description",
                new BigDecimal("99.99"),
                10
        );

        product = new Product();
        product.setId(1L);
        product.setSkuCode("TEST-SKU");
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setQuantity(10);
    }

    @Test
    void createProduct_ShouldCreateProduct_WhenSkuCodeDoesNotExist() {
        // Arrange
        when(productRepository.findBySkuCode(anyString()))
                .thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class)))
                .thenReturn(product);
        when(inventoryClient.getInventoryBySkuCode(anyString()))
                .thenReturn(ResponseEntity.ok(new InventoryResponse(1L, "TEST-SKU", 10)));

        // Act
        ProductResponse response = productService.createProduct(productRequest);

        // Assert
        assertNotNull(response);
        assertEquals("TEST-SKU", response.skuCode());
        assertEquals("Test Product", response.name());
        assertEquals(0, new BigDecimal("99.99").compareTo(response.price()));
        assertEquals(10, response.quantity());
        verify(productRepository, times(1)).findBySkuCode("TEST-SKU");
        verify(productRepository, times(1)).save(any(Product.class));
        verify(producer, times(1)).sendProductCreatedEvent("TEST-SKU", 10);
    }

    @Test
    void createProduct_ShouldThrowException_WhenSkuCodeAlreadyExists() {
        // Arrange
        when(productRepository.findBySkuCode(anyString()))
                .thenReturn(Optional.of(product));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            productService.createProduct(productRequest)
        );

        assertEquals("SKU code already exists: TEST-SKU", exception.getMessage());
        verify(productRepository, times(1)).findBySkuCode("TEST-SKU");
        verify(productRepository, never()).save(any(Product.class));
        verify(producer, never()).sendProductCreatedEvent(anyString(), anyInt());
    }

    @Test
    void getAllProducts_ShouldReturnAllProducts() {
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

        when(productRepository.findAll())
                .thenReturn(Arrays.asList(product1, product2));
        when(inventoryClient.getInventoryBySkuCode("SKU-1"))
                .thenReturn(ResponseEntity.ok(new InventoryResponse(1L, "SKU-1", 5)));
        when(inventoryClient.getInventoryBySkuCode("SKU-2"))
                .thenReturn(ResponseEntity.ok(new InventoryResponse(2L, "SKU-2", 10)));

        // Act
        List<ProductResponse> responses = productService.getAllProducts();

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("SKU-1", responses.get(0).skuCode());
        assertEquals("SKU-2", responses.get(1).skuCode());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProductById_ShouldReturnProduct_WhenProductExists() {
        // Arrange
        when(productRepository.findById(anyLong()))
                .thenReturn(Optional.of(product));
        when(inventoryClient.getInventoryBySkuCode(anyString()))
                .thenReturn(ResponseEntity.ok(new InventoryResponse(1L, "TEST-SKU", 10)));

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("TEST-SKU", response.skuCode());
        assertEquals("Test Product", response.name());
        verify(productRepository, times(1)).findById(1L);
    }
}
