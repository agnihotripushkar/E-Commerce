package com.pushkar.ecommerce.paymentservice.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID orderId,
        UUID userId,
        UUID paymentId,
        String status,
        BigDecimal amount
) {}
