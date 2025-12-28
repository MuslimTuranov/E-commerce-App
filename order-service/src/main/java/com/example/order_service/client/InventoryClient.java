package com.example.order_service.client;

import com.example.order_service.external.dto.InventoryRequest;
import com.example.order_service.external.dto.InventoryResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.service.annotation.PostExchange;

public interface InventoryClient {

    Logger log = LoggerFactory.getLogger(InventoryClient.class);

    @GetExchange("/api/inventory")
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    @Retry(name = "inventory")
    boolean isInStock(@RequestParam String skuCode, @RequestParam Integer quantity);

    default boolean fallbackMethod(String skuCode, Integer quantity, Throwable throwable) {
        log.info("Cannot get inventory for skuCode{}, failure reason: {}", skuCode, throwable.getMessage());
        return false;
    }

    @PostExchange("/api/inventory/decrease")
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackDecreaseInventory")
    @Retry(name = "inventory")
    ResponseEntity<InventoryResponse> decreaseInventory(@RequestBody InventoryRequest request);

    default ResponseEntity<InventoryResponse> fallbackDecreaseInventory(InventoryRequest request, Throwable throwable) {
        log.error("Fallback triggered for decreaseInventory for skuCode: {}. Error: {}", 
                 request.skuCode(), 
                 throwable != null ? throwable.getMessage() : "Unknown error",
                 throwable);

        InventoryResponse response = new InventoryResponse(
            null,
            request.skuCode(),
            0
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
