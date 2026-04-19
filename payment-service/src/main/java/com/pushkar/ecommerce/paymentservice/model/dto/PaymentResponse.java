package com.pushkar.ecommerce.paymentservice.model.dto;

import com.pushkar.ecommerce.paymentservice.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime createdAt
) {}
