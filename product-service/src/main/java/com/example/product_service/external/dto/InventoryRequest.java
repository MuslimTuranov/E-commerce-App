package com.example.product_service.external.dto;

public record InventoryRequest(
        String skuCode,
        Integer quantity
) {}
