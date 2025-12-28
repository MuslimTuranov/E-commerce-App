package com.example.order_service.dto;

import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        String orderNumber,
        String skuCode,
        Integer quantity,
        BigDecimal price
) {
}
