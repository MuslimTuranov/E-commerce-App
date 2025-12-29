package com.example.order_service.service;

import com.example.order_service.client.InventoryClient;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.event.OrderPlacedEvent;
import com.example.order_service.external.dto.InventoryRequest;
import com.example.order_service.external.dto.InventoryResponse;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest orderRequest;
    private Order order;

    @BeforeEach
    void setUp() {
        OrderRequest.UserDetails details = new OrderRequest.UserDetails("Ivan", "Ivanov", "ivan@mail.com");
        this.orderRequest = new OrderRequest("TEST-SKU", 10, details);

        order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORDER-123");
        order.setSkuCode("TEST-SKU");
        order.setQuantity(10);
    }

    @Test
    void placeOrder_ShouldReturnFalse_WhenProductOutOfStock() {
        when(inventoryClient.isInStock(anyString(), anyInt())).thenReturn(false);

        boolean result = orderService.placeOrder(orderRequest);

        assertFalse(result);
        verify(inventoryClient, times(1)).isInStock("TEST-SKU", 10);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_ShouldReturnTrue_WhenProductInStock() {
        when(inventoryClient.isInStock(anyString(), anyInt())).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(inventoryClient.decreaseInventory(any(InventoryRequest.class)))
                .thenReturn(ResponseEntity.ok(new InventoryResponse(1L, "TEST-SKU", 10)));

        boolean result = orderService.placeOrder(orderRequest);

        assertTrue(result);
        verify(inventoryClient, times(1)).isInStock("TEST-SKU", 10);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void placeOrder_ShouldCreateOrderWithCorrectDetails() {
        when(inventoryClient.isInStock(anyString(), anyInt())).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order orderToSave = invocation.getArgument(0);
            orderToSave.setId(1L);
            return orderToSave;
        });
        when(inventoryClient.decreaseInventory(any(InventoryRequest.class)))
                .thenReturn(ResponseEntity.ok(new InventoryResponse(1L, "TEST-SKU", 10)));

        boolean result = orderService.placeOrder(orderRequest);

        assertTrue(result);
        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getSkuCode().equals("TEST-SKU") &&
                        savedOrder.getQuantity() == 10 &&
                        savedOrder.getOrderNumber() != null
        ));
    }
}