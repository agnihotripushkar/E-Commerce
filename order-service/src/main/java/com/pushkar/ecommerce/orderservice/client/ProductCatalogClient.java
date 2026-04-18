package com.pushkar.ecommerce.orderservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ProductCatalogClient {

    private final RestClient productRestClient;

    public ProductSnapshot getProduct(Long productId) {
        return productRestClient.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .body(ProductSnapshot.class);
    }
}
