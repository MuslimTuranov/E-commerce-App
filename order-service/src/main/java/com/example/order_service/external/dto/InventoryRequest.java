package com.example.order_service.external.dto;


public record InventoryRequest(
        String skuCode,
        Integer quantity
) {}
