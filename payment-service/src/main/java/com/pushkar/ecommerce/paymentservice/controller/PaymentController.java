package com.pushkar.ecommerce.paymentservice.controller;

import com.pushkar.ecommerce.paymentservice.model.dto.PaymentResponse;
import com.pushkar.ecommerce.paymentservice.service.PaymentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentQueryService paymentQueryService;

    @GetMapping("/order/{orderId}")
    public PaymentResponse getByOrderId(@PathVariable UUID orderId) {
        return paymentQueryService.getByOrderId(orderId);
    }
}
