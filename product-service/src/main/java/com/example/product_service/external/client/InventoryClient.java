package com.example.product_service.external.client;

import com.example.product_service.external.dto.InventoryRequest;
import com.example.product_service.external.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "inventory-service", url = "${inventory.service.url}")
public interface InventoryClient {

    @PostMapping("/api/inventory/updateQuantity")
    ResponseEntity<InventoryResponse> upsertInventory(@RequestBody InventoryRequest request);

    @GetMapping("/api/inventory/skuCode")
    ResponseEntity<InventoryResponse> getInventoryBySkuCode(@RequestParam("skuCode") String skuCode);
}