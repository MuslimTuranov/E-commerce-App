package com.example.inventoryservice.kafka;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    public InventoryEventConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "inventory-events", groupId = "inventory-group")
    public void consume(String message) {
        log.info("Received inventory event: {}", message);
    }

    @KafkaListener(topics = "product-events", groupId = "inventory-group")
    public void consumeProductEvents(String message) {
        log.info("Получено событие от Product Service: {}", message);

        try {
            if (message.startsWith("CREATED:")) {
                String[] parts = message.split(":");
                String skuCode = parts[1];
                Integer quantity = Integer.parseInt(parts[2]);

                inventoryService.upsertInventory(new InventoryRequest(skuCode, quantity));
                log.info("Инвентарь для {} успешно инициализирован (кол-во: {})", skuCode, quantity);
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке события Kafka: {}", e.getMessage());
        }
    }
}
