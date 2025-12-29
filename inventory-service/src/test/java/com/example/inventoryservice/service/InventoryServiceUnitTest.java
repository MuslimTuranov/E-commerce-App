package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.kafka.InventoryEventProducer;
import com.example.inventoryservice.model.Inventory;
import com.example.inventoryservice.repository.InventoryRepository;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceUnitTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryEventProducer inventoryEventProducer;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryRequest request;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        request = new InventoryRequest("TEST-SKU", 10);
        inventory = new Inventory(1L, "TEST-SKU", 15);
    }

    @Test
    void isInStock_ShouldReturnTrue_WhenSufficientQuantityExists() {
        // Arrange
        when(inventoryRepository.existsBySkuCodeAndQuantityIsGreaterThanEqual(anyString(), anyInt()))
                .thenReturn(true);

        // Act
        boolean result = inventoryService.isInStock("TEST-SKU", 5);

        // Assert
        assertTrue(result);
        verify(inventoryRepository, times(1))
                .existsBySkuCodeAndQuantityIsGreaterThanEqual("TEST-SKU", 5);
    }

    @Test
    void isInStock_ShouldReturnFalse_WhenInsufficientQuantityExists() {
        // Arrange
        when(inventoryRepository.existsBySkuCodeAndQuantityIsGreaterThanEqual(anyString(), anyInt()))
                .thenReturn(false);

        // Act
        boolean result = inventoryService.isInStock("TEST-SKU", 20);

        // Assert
        assertFalse(result);
        verify(inventoryRepository, times(1))
                .existsBySkuCodeAndQuantityIsGreaterThanEqual("TEST-SKU", 20);
    }

    @Test
    void upsertInventory_ShouldCreateNewInventory_WhenSkuCodeDoesNotExist() {
        // Arrange
        when(inventoryRepository.increaseInventoryQuantity(anyString(), anyInt()))
                .thenReturn(0);
        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(inventory);

        // Act
        InventoryResponse response = inventoryService.upsertInventory(request);

        // Assert
        assertNotNull(response);
        assertEquals("TEST-SKU", response.skuCode());
        assertEquals(10, response.quantity());
        verify(inventoryRepository, times(1))
                .increaseInventoryQuantity("TEST-SKU", 10);
        verify(inventoryRepository, times(1))
                .save(any(Inventory.class));
        verify(inventoryEventProducer, times(1))
                .sendInventoryUpdatedEvent("TEST-SKU", 10);
    }

    @Test
    void upsertInventory_ShouldUpdateExistingInventory_WhenSkuCodeExists() {
        // Arrange
        when(inventoryRepository.increaseInventoryQuantity(anyString(), anyInt()))
                .thenReturn(1);
        when(inventoryRepository.findBySkuCode(anyString()))
                .thenReturn(Optional.of(inventory));

        // Act
        InventoryResponse response = inventoryService.upsertInventory(request);

        // Assert
        assertNotNull(response);
        assertEquals("TEST-SKU", response.skuCode());
        assertEquals(15, response.quantity());
        verify(inventoryRepository, times(1))
                .increaseInventoryQuantity("TEST-SKU", 10);
        verify(inventoryRepository, times(1))
                .findBySkuCode("TEST-SKU");
        verify(inventoryEventProducer, times(1))
                .sendInventoryUpdatedEvent("TEST-SKU", 15);
    }
}
