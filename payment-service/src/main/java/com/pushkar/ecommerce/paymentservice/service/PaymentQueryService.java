package com.pushkar.ecommerce.paymentservice.service;

import com.pushkar.ecommerce.paymentservice.exception.ResourceNotFoundException;
import com.pushkar.ecommerce.paymentservice.model.dto.PaymentResponse;
import com.pushkar.ecommerce.paymentservice.model.entity.Payment;
import com.pushkar.ecommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(UUID orderId) {
        Payment p = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getStatus(), p.getCreatedAt());
    }
}
