package com.pushkar.ecommerce.orderservice.model.dto;

import com.pushkar.ecommerce.orderservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderLineResponse> lineItems,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
