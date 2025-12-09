package com.example.product_service.dto;

import java.math.BigDecimal;

public record ProductRequest(String skuCode, String name, String description, BigDecimal price, Integer quantity) {
}

