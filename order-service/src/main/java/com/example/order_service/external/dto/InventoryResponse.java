package com.example.order_service.external.dto;


public record InventoryResponse(
        Long id,
        String skuCode,
        Integer quantity
) {}
