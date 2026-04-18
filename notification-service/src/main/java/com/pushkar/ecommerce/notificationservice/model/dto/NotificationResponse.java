package com.pushkar.ecommerce.notificationservice.model.dto;

import com.pushkar.ecommerce.notificationservice.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        UUID orderId,
        NotificationType notificationType,
        String message,
        LocalDateTime createdAt
) {}
