package com.pushkar.ecommerce.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.ecommerce.notificationservice.kafka.OrderCancelledEvent;
import com.pushkar.ecommerce.notificationservice.kafka.OrderPlacedEvent;
import com.pushkar.ecommerce.notificationservice.kafka.OrderShippedEvent;
import com.pushkar.ecommerce.notificationservice.kafka.PaymentProcessedEvent;
import com.pushkar.ecommerce.notificationservice.model.NotificationType;
import com.pushkar.ecommerce.notificationservice.model.entity.UserNotification;
import com.pushkar.ecommerce.notificationservice.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
            save(event.userId(), event.orderId(), NotificationType.ORDER_PLACED,
                    "Your order " + event.orderId() + " has been placed. Total: " + event.totalAmount());
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
            boolean success = "SUCCESS".equalsIgnoreCase(event.status());
            NotificationType type = success
                    ? NotificationType.PAYMENT_CONFIRMED
                    : NotificationType.PAYMENT_FAILED;
            String message = success
                    ? "Payment confirmed for order " + event.orderId() + ". Amount: " + event.amount()
                    : "Payment failed for order " + event.orderId() + ". Please contact support.";
            save(event.userId(), event.orderId(), type, message);
        } catch (JsonProcessingException e) {
            log.error("Invalid payment-processed payload: {}", payload, e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topic.order-shipped:order-shipped}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onOrderShipped(String payload) {
        try {
            OrderShippedEvent event = objectMapper.readValue(payload, OrderShippedEvent.class);
            String tracking = event.trackingNumber() != null
                    ? " Tracking: " + event.trackingNumber() : "";
            save(event.userId(), event.orderId(), NotificationType.ORDER_SHIPPED,
                    "Your order " + event.orderId() + " is on the way!" + tracking);
        } catch (JsonProcessingException e) {
            log.error("Invalid order-shipped payload: {}", payload, e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topic.order-cancelled:order-cancelled}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onOrderCancelled(String payload) {
        try {
            OrderCancelledEvent event = objectMapper.readValue(payload, OrderCancelledEvent.class);
            save(event.userId(), event.orderId(), NotificationType.ORDER_CANCELLED,
                    "Your order " + event.orderId() + " has been cancelled.");
        } catch (JsonProcessingException e) {
            log.error("Invalid order-cancelled payload: {}", payload, e);
        }
    }

    private void save(UUID userId, UUID orderId, NotificationType type, String message) {
        UserNotification n = new UserNotification();
        n.setUserId(userId);
        n.setOrderId(orderId);
        n.setNotificationType(type);
        n.setMessage(message);
        notificationRepository.save(n);
        log.info("Notification saved: userId={} type={}", userId, type);
    }
}
