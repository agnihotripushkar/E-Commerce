package com.pushkar.ecommerce.notificationservice.kafka;

import java.util.UUID;

public record OrderShippedEvent(
        UUID orderId,
        UUID userId,
        String trackingNumber
) {}
