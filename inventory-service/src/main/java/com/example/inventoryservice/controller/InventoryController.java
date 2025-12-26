package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryController.class);

    public InventoryController(InventoryRepository inventoryRepository, InventoryService inventoryService) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public boolean isInStock(@RequestParam String skuCode, @RequestParam Integer quantity) {
        return inventoryService.isInStock(skuCode, quantity);
    }

    @PostMapping("/updateQuantity")
    public ResponseEntity<InventoryResponse> upsertInventory(@RequestBody InventoryRequest request) {

        boolean alreadyExists = inventoryRepository.findBySkuCode(request.skuCode()).isPresent();

        InventoryResponse response = inventoryService.upsertInventory(request);

        if (!alreadyExists) {
            log.info("Created new inventory entry for SKU: {}", request.skuCode());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            log.info("Updated existing inventory for SKU: {}", request.skuCode());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/skuCode")
    public ResponseEntity<InventoryResponse> getInventoryBySkuCode(@RequestParam String skuCode) {
        return ResponseEntity.ok(inventoryService.getInventoryBySkuCode(skuCode));
    }

    @PostMapping("/decrease")
    public ResponseEntity<InventoryResponse> decreaseInventory(@RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.decreaseInventory(request));
    }
}
