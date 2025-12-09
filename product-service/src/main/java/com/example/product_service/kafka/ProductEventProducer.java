package com.example.product_service.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "product-events";

    public ProductEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendProductCreatedEvent(String skuCode) {
        kafkaTemplate.send(TOPIC, "Product created: " + skuCode);
    }

    public void sendProductUpdatedEvent(String skuCode) {
        kafkaTemplate.send(TOPIC, "Product updated: " + skuCode);
    }

    public void sendProductDeletedEvent(String skuCode) {
        kafkaTemplate.send(TOPIC, "Product deleted: " + skuCode);
    }
}
