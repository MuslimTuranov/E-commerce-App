package com.example.inventoryservice.dto;


public record InventoryRequest(
        String skuCode,
        Integer quantity
) {}
