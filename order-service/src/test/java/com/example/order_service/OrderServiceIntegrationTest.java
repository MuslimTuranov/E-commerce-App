package com.example.order_service;

import com.example.order_service.client.InventoryClient;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class OrderServiceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private InventoryClient inventoryClient; // Mock the client

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        // Mock the isInStock method to always return true
        when(inventoryClient.isInStock("TEST-SKU", 5)).thenReturn(true);
    }

    @Test
    void placeOrder_ShouldReturnCreatedStatus() throws Exception {
        OrderRequest.UserDetails details = new OrderRequest.UserDetails("test@mail.com", "Test", "User");
        OrderRequest request = new OrderRequest("TEST-SKU", 5, details);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/order", entity, String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        // Since your controller returns plain string, adjust parsing
        assertTrue(response.getBody().contains("Order placed successfully"));
    }

    @Test
    void getAllOrders_ShouldReturnOrders() throws Exception {
        Order order1 = new Order();
        order1.setOrderNumber("ORDER-1");
        order1.setSkuCode("SKU-1");
        order1.setQuantity(5);

        Order order2 = new Order();
        order2.setOrderNumber("ORDER-2");
        order2.setSkuCode("SKU-2");
        order2.setQuantity(10);

        orderRepository.save(order1);
        orderRepository.save(order2);

        ResponseEntity<Order[]> response = restTemplate.getForEntity("/api/order", Order[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().length);

        assertEquals("SKU-1", response.getBody()[0].getSkuCode());
        assertEquals("SKU-2", response.getBody()[1].getSkuCode());
    }
}
