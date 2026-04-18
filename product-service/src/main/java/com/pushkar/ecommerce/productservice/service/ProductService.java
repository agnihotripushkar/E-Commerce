package com.pushkar.ecommerce.productservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.ecommerce.productservice.exception.ResourceNotFoundException;
import com.pushkar.ecommerce.productservice.model.dto.CreateProductRequest;
import com.pushkar.ecommerce.productservice.model.dto.ProductResponse;
import com.pushkar.ecommerce.productservice.model.dto.UpdateProductRequest;
import com.pushkar.ecommerce.productservice.model.entity.Product;
import com.pushkar.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Duration PRODUCT_CACHE_TTL = Duration.ofMinutes(10);

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        String cacheKey = cacheKey(id);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, ProductResponse.class);
            } catch (JsonProcessingException ignored) {
                redisTemplate.delete(cacheKey);
            }
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        ProductResponse response = toResponse(product);
        cacheProduct(response);
        return response;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new IllegalArgumentException("SKU already exists: " + request.sku());
        }

        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(request.category());
        product.setActive(request.active() == null || request.active());

        Product saved = productRepository.save(product);
        ProductResponse response = toResponse(saved);
        cacheProduct(response);
        return response;
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null) product.setPrice(request.price());
        if (request.stock() != null) product.setStock(request.stock());
        if (request.category() != null) product.setCategory(request.category());
        if (request.active() != null) product.setActive(request.active());

        Product saved = productRepository.save(product);
        ProductResponse response = toResponse(saved);
        cacheProduct(response);
        return response;
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        productRepository.delete(product);
        redisTemplate.delete(cacheKey(id));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private void cacheProduct(ProductResponse productResponse) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey(productResponse.id()),
                    objectMapper.writeValueAsString(productResponse),
                    PRODUCT_CACHE_TTL
            );
        } catch (JsonProcessingException ignored) {
            // Ignore cache serialization issues; source of truth remains PostgreSQL.
        }
    }

    private String cacheKey(Long id) {
        return "product:" + id;
    }
}
