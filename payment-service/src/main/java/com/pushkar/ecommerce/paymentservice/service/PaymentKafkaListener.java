package com.pushkar.ecommerce.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.ecommerce.paymentservice.kafka.OrderPlacedEvent;
import com.pushkar.ecommerce.paymentservice.kafka.PaymentProcessedEvent;
import com.pushkar.ecommerce.paymentservice.model.PaymentStatus;
import com.pushkar.ecommerce.paymentservice.model.entity.Payment;
import com.pushkar.ecommerce.paymentservice.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Consumes order-placed events and simulates payment processing.
 *
 * Circuit breaker pattern (Resilience4j):
 *  - Wraps the mock gateway call with @CircuitBreaker(name = "paymentGateway")
 *  - Config: slidingWindowSize=10, failureRateThreshold=50%, waitInOpenState=30s
 *  - Fallback: persists FAILED payment and routes to payment-dlq for manual retry
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaListener {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.payment-processed:payment-processed}")
    private String paymentProcessedTopic;

    @Value("${kafka.topic.payment-dlq:payment-dlq}")
    private String paymentDlqTopic;

    @KafkaListener(
            topics = "${kafka.topic.order-placed:order-placed}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onOrderPlaced(String payload) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(payload, OrderPlacedEvent.class);
            if (paymentRepository.existsByOrderId(event.orderId())) {
                log.info("Duplicate order-placed event ignored for orderId={}", event.orderId());
                return;
            }
            processPayment(event);
        } catch (JsonProcessingException e) {
            log.error("Invalid order-placed payload: {}", payload, e);
        }
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "paymentFallback")
    void processPayment(OrderPlacedEvent event) {
        // Simulate mock payment gateway call (always succeeds in mock)
        boolean gatewaySuccess = simulateMockGateway(event.orderId());

        Payment payment = new Payment();
        payment.setOrderId(event.orderId());
        payment.setAmount(event.totalAmount());
        payment.setStatus(gatewaySuccess ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
        Payment saved = paymentRepository.save(payment);

        PaymentProcessedEvent out = new PaymentProcessedEvent(
                event.orderId(),
                event.userId(),
                saved.getId(),
                gatewaySuccess ? "SUCCESS" : "FAILED",
                event.totalAmount()
        );
        publish(paymentProcessedTopic, event.orderId().toString(), out);
        log.info("Payment {} for order {} — status={}", saved.getId(), event.orderId(), saved.getStatus());
    }

    void paymentFallback(OrderPlacedEvent event, Throwable ex) {
        log.error("Circuit breaker OPEN — routing order {} to DLQ. Reason: {}",
                event.orderId(), ex.getMessage());

        // Persist failed payment record
        Payment payment = new Payment();
        payment.setOrderId(event.orderId());
        payment.setAmount(event.totalAmount());
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        // Send to dead-letter queue for manual inspection / retry
        try {
            kafkaTemplate.send(paymentDlqTopic, event.orderId().toString(),
                    objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("Failed to send event to DLQ for orderId={}", event.orderId(), e);
        }

        // Notify order service of payment failure
        PaymentProcessedEvent failedEvent = new PaymentProcessedEvent(
                event.orderId(), event.userId(), null, "FAILED", event.totalAmount());
        publish(paymentProcessedTopic, event.orderId().toString(), failedEvent);
    }

    /**
     * Mock payment gateway — simulates a realistic success rate.
     * In production, replace with an HTTP call to Stripe / PayPal.
     */
    private boolean simulateMockGateway(UUID orderId) {
        // 90% success rate for mock; circuit breaker trips if failures exceed 50%
        return Math.random() > 0.1;
    }

    private void publish(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for topic={}", topic, e);
        }
    }
}
