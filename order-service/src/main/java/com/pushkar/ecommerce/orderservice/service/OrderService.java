package com.pushkar.ecommerce.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.ecommerce.orderservice.client.ProductCatalogClient;
import com.pushkar.ecommerce.orderservice.client.ProductSnapshot;
import com.pushkar.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.pushkar.ecommerce.orderservice.kafka.OrderCancelledEvent;
import com.pushkar.ecommerce.orderservice.kafka.OrderPlacedEvent;
import com.pushkar.ecommerce.orderservice.kafka.PaymentProcessedEvent;
import com.pushkar.ecommerce.orderservice.model.OrderStatus;
import com.pushkar.ecommerce.orderservice.model.dto.OrderLineResponse;
import com.pushkar.ecommerce.orderservice.model.dto.OrderResponse;
import com.pushkar.ecommerce.orderservice.model.dto.PlaceOrderRequest;
import com.pushkar.ecommerce.orderservice.model.entity.Order;
import com.pushkar.ecommerce.orderservice.model.entity.OrderLineItem;
import com.pushkar.ecommerce.orderservice.repository.OrderRepository;
import io.github.resilience4j.retry.annotation.Retry;
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

    @Value("${kafka.topic.order-cancelled:order-cancelled}")
    private String orderCancelledTopic;

    // ── Queries ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrdersForUser(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    // ── Commands ─────────────────────────────────────────────────────────────────

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
            total = total.add(unit.multiply(BigDecimal.valueOf(line.quantity())));

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
        publishOrderPlaced(saved);
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        Order order = findOrThrow(id);
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cannot cancel order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        publishOrderCancelled(saved);
        return toResponse(saved);
    }

    // ── Kafka consumer ────────────────────────────────────────────────────────────

    @KafkaListener(
            topics = "${kafka.topic.payment-processed:payment-processed}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onPaymentProcessed(String payload) {
        try {
            PaymentProcessedEvent event = objectMapper.readValue(payload, PaymentProcessedEvent.class);
            orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
                order.setStatus("SUCCESS".equalsIgnoreCase(event.status())
                        ? OrderStatus.PAID : OrderStatus.PAYMENT_FAILED);
                orderRepository.save(order);
                log.info("Order {} status updated to {} after payment", order.getId(), order.getStatus());
            }, () -> log.warn("Payment event for unknown order: {}", event.orderId()));
        } catch (JsonProcessingException e) {
            log.error("Invalid payment-processed payload: {}", payload, e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    @Retry(name = "productClient", fallbackMethod = "fetchProductFallback")
    ProductSnapshot fetchProduct(Long productId) {
        try {
            return productCatalogClient.getProduct(productId);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new IllegalArgumentException("Product not found: " + productId);
            }
            throw e;
        }
    }

    ProductSnapshot fetchProductFallback(Long productId, Exception ex) {
        log.error("Product service unavailable for id={} after retries: {}", productId, ex.getMessage());
        throw new IllegalStateException("Product service temporarily unavailable", ex);
    }

    private void publishOrderPlaced(Order order) {
        OrderPlacedEvent event = new OrderPlacedEvent(
                order.getId(), order.getUserId(), order.getTotalAmount(),
                order.getLineItems().stream()
                        .map(li -> new OrderPlacedEvent.OrderPlacedLine(
                                li.getProductId(), li.getQuantity(), li.getUnitPrice()))
                        .toList());
        publish(orderPlacedTopic, order.getId().toString(), event);
    }

    private void publishOrderCancelled(Order order) {
        OrderCancelledEvent event = new OrderCancelledEvent(
                order.getId(), order.getUserId(), "User requested cancellation");
        publish(orderCancelledTopic, order.getId().toString(), event);
    }

    private void publish(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for topic={}", topic, e);
        }
    }

    private Order findOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderLineResponse> lines = order.getLineItems().stream()
                .map(li -> new OrderLineResponse(li.getProductId(), li.getQuantity(), li.getUnitPrice()))
                .toList();
        return new OrderResponse(order.getId(), order.getUserId(), order.getStatus(),
                order.getTotalAmount(), lines, order.getCreatedAt(), order.getUpdatedAt());
    }
}
