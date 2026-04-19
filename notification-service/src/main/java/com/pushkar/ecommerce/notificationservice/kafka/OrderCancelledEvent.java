package com.pushkar.ecommerce.notificationservice.kafka;

import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        UUID userId,
        String reason
) {}
