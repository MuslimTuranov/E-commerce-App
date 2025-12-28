package com.example.order_service.kafka;

import com.example.order_service.event.OrderPlacedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "order-service-group")
    public void consumeOrderEvent(OrderPlacedEvent orderPlacedEvent) {
        // Process the order event
        System.out.println("Received order event: " + orderPlacedEvent.getOrderNumber());

        // Here you could add logic to:
        // 1. Send email notifications
        // 2. Update other services
        // 3. Log the event for analytics
        // 4. Trigger fulfillment processes

        System.out.println("Order event processed successfully");
    }

    @KafkaListener(topics = "inventory-events", groupId = "order-service-group", containerFactory = "stringKafkaListenerContainerFactory")
    public void consumeInventoryEvent(String inventoryEvent) {
        // Consume inventory events to handle stock updates that might affect orders
        System.out.println("Received inventory event: " + inventoryEvent);

        // Here you could add logic to:
        // 1. Check if any pending orders can now be fulfilled
        // 2. Update order statuses based on inventory changes
        // 3. Notify customers about stock availability
    }
}
