package com.pushkar.ecommerce.notificationservice.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID orderId,
        UUID userId,
        UUID paymentId,
        String status,
        BigDecimal amount
) {}
