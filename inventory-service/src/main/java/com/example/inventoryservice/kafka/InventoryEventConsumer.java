package com.example.inventoryservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    @KafkaListener(topics = "inventory-events", groupId = "inventory-group")
    public void consume(String message) {
        log.info("Received inventory event: {}", message);
    }

    @KafkaListener(topics = "product-events", groupId = "inventory-group")
    public void consumeProductEvents(String message) {
        log.info("Received product event in inventory service: {}", message);
        // Here you could add logic to react to product events
        // For example, when a product is created, you might want to initialize inventory
    }
}
