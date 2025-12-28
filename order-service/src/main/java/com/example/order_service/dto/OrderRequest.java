package com.example.order_service.dto;

import java.math.BigDecimal;

public record OrderRequest(Long id,
                           String orderNumber,
                           String skuCode,
                           String productId,
                           Integer quantity,
                           BigDecimal price,
                           UserDetails userDetails) {
    public record UserDetails(String email, String firstName, String lastName) {}
}
