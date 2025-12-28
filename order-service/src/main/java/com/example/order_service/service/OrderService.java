package com.example.order_service.service;

import com.example.order_service.client.InventoryClient;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.event.OrderPlacedEvent;
import com.example.order_service.external.dto.InventoryRequest;
import com.example.order_service.external.dto.InventoryResponse;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.UUID;


@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public boolean placeOrder(OrderRequest orderRequest) {

        boolean isInStock =
                inventoryClient.isInStock(orderRequest.skuCode(), orderRequest.quantity());

        if (!isInStock) {
            log.warn("Product {} is out of stock", orderRequest.skuCode());
            return false;
        }

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setSkuCode(orderRequest.skuCode());
        order.setQuantity(orderRequest.quantity());

        orderRepository.save(order);

        inventoryClient.decreaseInventory(
                new InventoryRequest(orderRequest.skuCode(), orderRequest.quantity())
        );

        OrderPlacedEvent event =
                new OrderPlacedEvent(order.getOrderNumber());

        kafkaTemplate.send("order-placed", event);

        log.info("Order placed successfully {}", order.getOrderNumber());

        return true;
    }
}