package com.example.order_service.kafka;

import com.example.order_service.event.OrderPlacedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private static final String TOPIC = "order-events";

    public OrderEventProducer(KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderPlacedEvent(OrderPlacedEvent orderPlacedEvent) {
        kafkaTemplate.send(TOPIC, orderPlacedEvent);
    }

    public void sendOrderUpdatedEvent(String orderNumber, String status) {
        OrderPlacedEvent event = new OrderPlacedEvent(orderNumber, "Order " + orderNumber + " status updated to: " + status);
        kafkaTemplate.send(TOPIC, event);
    }

    public void sendOrderCancelledEvent(String orderNumber) {
        OrderPlacedEvent event = new OrderPlacedEvent(orderNumber, "Order " + orderNumber + " has been cancelled");
        kafkaTemplate.send(TOPIC, event);
    }
}
