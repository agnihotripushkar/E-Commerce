package com.pushkar.ecommerce.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.ecommerce.notificationservice.kafka.OrderPlacedEvent;
import com.pushkar.ecommerce.notificationservice.kafka.PaymentProcessedEvent;
import com.pushkar.ecommerce.notificationservice.model.NotificationType;
import com.pushkar.ecommerce.notificationservice.model.entity.UserNotification;
import com.pushkar.ecommerce.notificationservice.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaListener {

    private final UserNotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topic.order-placed:order-placed}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onOrderPlaced(String payload) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(payload, OrderPlacedEvent.class);
            UserNotification n = new UserNotification();
            n.setUserId(event.userId());
            n.setOrderId(event.orderId());
            n.setNotificationType(NotificationType.ORDER_PLACED);
            n.setMessage("Your order " + event.orderId() + " was placed. Total: " + event.totalAmount());
            notificationRepository.save(n);
        } catch (JsonProcessingException e) {
            log.error("Invalid order-placed payload: {}", payload, e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topic.payment-processed:payment-processed}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onPaymentProcessed(String payload) {
        try {
            PaymentProcessedEvent event = objectMapper.readValue(payload, PaymentProcessedEvent.class);
            if (!"SUCCESS".equalsIgnoreCase(event.status())) {
                return;
            }
            UserNotification n = new UserNotification();
            n.setUserId(event.userId());
            n.setOrderId(event.orderId());
            n.setNotificationType(NotificationType.PAYMENT_CONFIRMED);
            n.setMessage("Payment confirmed for order " + event.orderId() + ". Amount: " + event.amount());
            notificationRepository.save(n);
        } catch (JsonProcessingException e) {
            log.error("Invalid payment-processed payload: {}", payload, e);
        }
    }
}
