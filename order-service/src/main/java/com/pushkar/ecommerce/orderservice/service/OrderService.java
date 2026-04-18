package com.pushkar.ecommerce.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.ecommerce.orderservice.client.ProductCatalogClient;
import com.pushkar.ecommerce.orderservice.client.ProductSnapshot;
import com.pushkar.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.pushkar.ecommerce.orderservice.kafka.OrderPlacedEvent;
import com.pushkar.ecommerce.orderservice.kafka.PaymentProcessedEvent;
import com.pushkar.ecommerce.orderservice.model.OrderStatus;
import com.pushkar.ecommerce.orderservice.model.dto.OrderLineResponse;
import com.pushkar.ecommerce.orderservice.model.dto.OrderResponse;
import com.pushkar.ecommerce.orderservice.model.dto.PlaceOrderRequest;
import com.pushkar.ecommerce.orderservice.model.entity.Order;
import com.pushkar.ecommerce.orderservice.model.entity.OrderLineItem;
import com.pushkar.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductCatalogClient productCatalogClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.order-placed:order-placed}")
    private String orderPlacedTopic;

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrdersForUser(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        List<OrderLineItem> lines = new ArrayList<>();

        for (var line : request.items()) {
            ProductSnapshot product = fetchProduct(line.productId());
            if (Boolean.FALSE.equals(product.active())) {
                throw new IllegalArgumentException("Product is not available: " + line.productId());
            }
            if (product.stock() != null && product.stock() < line.quantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + line.productId());
            }
            BigDecimal unit = product.price();
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(line.quantity()));
            total = total.add(lineTotal);

            OrderLineItem item = new OrderLineItem();
            item.setProductId(line.productId());
            item.setQuantity(line.quantity());
            item.setUnitPrice(unit);
            lines.add(item);
        }

        Order order = new Order();
        order.setUserId(request.userId());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(total);
        for (OrderLineItem line : lines) {
            line.setOrder(order);
            order.getLineItems().add(line);
        }

        Order saved = orderRepository.save(order);

        OrderPlacedEvent event = new OrderPlacedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getTotalAmount(),
                saved.getLineItems().stream()
                        .map(li -> new OrderPlacedEvent.OrderPlacedLine(
                                li.getProductId(), li.getQuantity(), li.getUnitPrice()))
                        .toList()
        );
        try {
            kafkaTemplate.send(orderPlacedTopic, saved.getId().toString(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize order event", e);
        }

        return toResponse(saved);
    }

    private ProductSnapshot fetchProduct(Long productId) {
        try {
            return productCatalogClient.getProduct(productId);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new IllegalArgumentException("Product not found: " + productId);
            }
            throw e;
        }
    }

    @KafkaListener(
            topics = "${kafka.topic.payment-processed:payment-processed}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onPaymentProcessed(String payload) {
        try {
            PaymentProcessedEvent event = objectMapper.readValue(payload, PaymentProcessedEvent.class);
            orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
                if ("SUCCESS".equalsIgnoreCase(event.status())) {
                    order.setStatus(OrderStatus.PAID);
                } else {
                    order.setStatus(OrderStatus.PAYMENT_FAILED);
                }
                orderRepository.save(order);
            }, () -> log.warn("Payment event for unknown order: {}", event.orderId()));
        } catch (JsonProcessingException e) {
            log.error("Invalid payment-processed payload: {}", payload, e);
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderLineResponse> lines = order.getLineItems().stream()
                .map(li -> new OrderLineResponse(li.getProductId(), li.getQuantity(), li.getUnitPrice()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                lines,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
