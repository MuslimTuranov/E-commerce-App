package com.example.product_service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    @KafkaListener(topics = "product-events", groupId = "product-group")
    public void consume(String message) {
        log.info("Received event: {}", message);
    }
}
