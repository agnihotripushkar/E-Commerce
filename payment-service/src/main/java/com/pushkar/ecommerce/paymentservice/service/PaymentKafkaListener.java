package com.pushkar.ecommerce.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.ecommerce.paymentservice.kafka.OrderPlacedEvent;
import com.pushkar.ecommerce.paymentservice.kafka.PaymentProcessedEvent;
import com.pushkar.ecommerce.paymentservice.model.PaymentStatus;
import com.pushkar.ecommerce.paymentservice.model.entity.Payment;
import com.pushkar.ecommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaListener {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.payment-processed:payment-processed}")
    private String paymentProcessedTopic;

    @KafkaListener(
            topics = "${kafka.topic.order-placed:order-placed}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onOrderPlaced(String payload) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(payload, OrderPlacedEvent.class);
            if (paymentRepository.existsByOrderId(event.orderId())) {
                return;
            }

            Payment payment = new Payment();
            payment.setOrderId(event.orderId());
            payment.setAmount(event.totalAmount());
            payment.setStatus(PaymentStatus.COMPLETED);
            Payment saved = paymentRepository.save(payment);

            PaymentProcessedEvent out = new PaymentProcessedEvent(
                    event.orderId(),
                    event.userId(),
                    saved.getId(),
                    "SUCCESS",
                    event.totalAmount()
            );
            kafkaTemplate.send(
                    paymentProcessedTopic,
                    event.orderId().toString(),
                    objectMapper.writeValueAsString(out));
        } catch (JsonProcessingException e) {
            log.error("Invalid order-placed payload: {}", payload, e);
        }
    }
}
