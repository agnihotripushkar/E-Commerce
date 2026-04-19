package com.pushkar.ecommerce.notificationservice.controller;

import com.pushkar.ecommerce.notificationservice.model.dto.NotificationResponse;
import com.pushkar.ecommerce.notificationservice.service.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping
    public List<NotificationResponse> list(@RequestParam UUID userId) {
        return notificationQueryService.listForUser(userId);
    }
}
