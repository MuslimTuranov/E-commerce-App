package com.example.product_service.external.dto;

public record InventoryResponse(
        Long id,
        String skuCode,
        Integer quantity
) {}