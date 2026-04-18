package com.pushkar.ecommerce.notificationservice.kafka;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID orderId,
        UUID userId,
        BigDecimal totalAmount,
        List<OrderPlacedLine> items
) {
    public record OrderPlacedLine(Long productId, Integer quantity, BigDecimal unitPrice) {}
}
