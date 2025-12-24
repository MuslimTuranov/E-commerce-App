package com.example.inventoryservice.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "inventory-events";

    public InventoryEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendInventoryUpdatedEvent(String skuCode, int quantity) {
        kafkaTemplate.send(TOPIC, "Inventory updated: " + skuCode + " - Quantity: " + quantity);
    }

    public void sendInventoryLowStockEvent(String skuCode, int quantity) {
        kafkaTemplate.send(TOPIC, "Low stock alert: " + skuCode + " - Quantity: " + quantity);
    }

    public void sendInventoryOutOfStockEvent(String skuCode) {
        kafkaTemplate.send(TOPIC, "Out of stock: " + skuCode);
    }
}
