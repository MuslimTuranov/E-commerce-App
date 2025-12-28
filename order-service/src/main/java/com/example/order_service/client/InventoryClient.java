package com.example.order_service.client;


import com.example.order_service.external.dto.InventoryRequest;
import com.example.order_service.external.dto.InventoryResponse;
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
    boolean isInStock(@RequestParam String skuCode, @RequestParam Integer quantity);

    default boolean fallbackMethod(String skuCode, Integer quantity, Throwable throwable) {
        // Log the error and return a default value
        log.info("Cannot get inventory for skuCode{}, failure reason: {}", skuCode, throwable.getMessage());
        return false; // or any other default behavior
    }

    @PostExchange("/api/inventory/decrease")
    ResponseEntity<InventoryResponse> decreaseInventory(@RequestBody InventoryRequest request);


}
