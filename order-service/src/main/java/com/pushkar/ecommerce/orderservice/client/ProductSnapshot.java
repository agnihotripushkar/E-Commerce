package com.pushkar.ecommerce.orderservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductSnapshot(
        Long id,
        BigDecimal price,
        Integer stock,
        Boolean active
) {}
