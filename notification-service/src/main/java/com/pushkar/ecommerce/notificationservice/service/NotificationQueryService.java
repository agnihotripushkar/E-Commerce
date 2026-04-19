package com.pushkar.ecommerce.notificationservice.service;

import com.pushkar.ecommerce.notificationservice.model.dto.NotificationResponse;
import com.pushkar.ecommerce.notificationservice.model.entity.UserNotification;
import com.pushkar.ecommerce.notificationservice.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final UserNotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(UserNotification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getOrderId(),
                n.getNotificationType(),
                n.getMessage(),
                n.getCreatedAt()
        );
    }
}
