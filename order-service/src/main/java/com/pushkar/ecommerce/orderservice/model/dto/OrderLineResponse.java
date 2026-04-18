package com.pushkar.ecommerce.orderservice.model.dto;

import java.math.BigDecimal;

public record OrderLineResponse(
        Long productId,
        Integer quantity,
        BigDecimal unitPrice
) {}
