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
        try {
            var isProductInStock = inventoryClient.isInStock(orderRequest.skuCode(), orderRequest.quantity());

            if (isProductInStock) {
                Order order = new Order();
                order.setOrderNumber(UUID.randomUUID().toString());
                order.setPrice(orderRequest.price());
                order.setQuantity(orderRequest.quantity());
                order.setSkuCode(orderRequest.skuCode());

                orderRepository.save(order);

                ResponseEntity<InventoryResponse> response = inventoryClient.decreaseInventory(
                        new InventoryRequest(orderRequest.skuCode(), orderRequest.quantity())
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    InventoryResponse inventoryResponse = response.getBody();
                    log.info("Inventory updated - SkuCode: {}, Quantity: {}",
                            inventoryResponse.skuCode(),
                            inventoryResponse.quantity()
                    );

                    OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent(order.getOrderNumber(), orderRequest.userDetails().email());
                    log.info("Start - Sending OrderPlacedEvent {} to Kafka topic order-placed", orderPlacedEvent);
                    kafkaTemplate.send("order-placed", orderPlacedEvent);
                    log.info("End - Sending OrderPlacedEvent {} to Kafka topic order-placed", orderPlacedEvent);

                    return true;
                } else {
                    log.error("Failed to update inventory. Status: {}", response.getStatusCode());
                    return false;
                }
            } else {
                log.warn("Product with skuCode {} is out of stock", orderRequest.skuCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Error occurred while processing order: {}", e.getMessage(), e);
            return false;
        }
    }
}