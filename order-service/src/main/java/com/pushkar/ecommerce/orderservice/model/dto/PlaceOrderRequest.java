package com.pushkar.ecommerce.orderservice.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(
        @NotNull(message = "userId is required")
        UUID userId,

        @NotEmpty(message = "At least one line item is required")
        @Valid
        List<PlaceOrderLineRequest> items
) {}
