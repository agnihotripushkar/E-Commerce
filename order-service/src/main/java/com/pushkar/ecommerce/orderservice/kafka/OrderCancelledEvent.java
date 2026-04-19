package com.pushkar.ecommerce.orderservice.kafka;

import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        UUID userId,
        String reason
) {}
