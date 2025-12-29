package com.example.order_service.dto;

import java.math.BigDecimal;

public record OrderRequest(String skuCode,
                           Integer quantity,
                           UserDetails userDetails) {
    public record UserDetails(String email, String firstName, String lastName) {}
}
