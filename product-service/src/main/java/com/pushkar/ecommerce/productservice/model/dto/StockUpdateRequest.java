package com.pushkar.ecommerce.productservice.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockUpdateRequest(
        @NotNull @Min(0) Integer delta  // positive = restock, negative = deduct
) {}
