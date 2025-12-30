package com.example.order_service.service;

import com.example.order_service.client.InventoryClient;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.event.OrderPlacedEvent;
import com.example.order_service.external.dto.InventoryRequest;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

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

    @BeforeEach
    void setUp() {
        OrderRequest.UserDetails details = new OrderRequest.UserDetails(
                "ivan@mail.com", "Ivan", "Ivanov"
        );
        orderRequest = new OrderRequest("TEST-SKU", 10, details);
    }

    @Test
    void placeOrder_ShouldReturnFalse_WhenProductOutOfStock() {
        when(inventoryClient.isInStock(anyString(), anyInt())).thenReturn(false);

        boolean result = orderService.placeOrder(orderRequest);

        assertFalse(result);
        verify(orderRepository, never()).save(any(Order.class));
        verify(kafkaTemplate, never()).send(anyString(), any(OrderPlacedEvent.class));
    }

    @Test
    void placeOrder_ShouldReturnTrue_WhenProductInStock() {
        when(inventoryClient.isInStock(anyString(), anyInt())).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = orderService.placeOrder(orderRequest);

        assertTrue(result);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(kafkaTemplate, times(1)).send(anyString(), any(OrderPlacedEvent.class));
        verify(inventoryClient, times(1)).decreaseInventory(any(InventoryRequest.class));
    }

    @Test
    void placeOrder_ShouldCreateOrderWithCorrectDetails() {
        when(inventoryClient.isInStock(anyString(), anyInt())).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.placeOrder(orderRequest);

        verify(orderRepository).save(argThat(order ->
                order.getSkuCode().equals("TEST-SKU") &&
                        order.getQuantity() == 10 &&
                        order.getOrderNumber() != null &&
                        order.getPrice().equals(BigDecimal.ZERO)
        ));
    }

    @Test
    void placeOrder_ShouldSendOrderPlacedEvent() {
        when(inventoryClient.isInStock(anyString(), anyInt())).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.placeOrder(orderRequest);

        verify(kafkaTemplate, times(1)).send(eq("order-placed"), argThat(event ->
                event.getOrderNumber() != null
        ));
    }
}
