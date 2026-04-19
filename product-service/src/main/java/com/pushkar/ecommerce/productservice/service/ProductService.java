package com.pushkar.ecommerce.productservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.ecommerce.productservice.exception.ResourceNotFoundException;
import com.pushkar.ecommerce.productservice.model.dto.CreateProductRequest;
import com.pushkar.ecommerce.productservice.model.dto.ProductResponse;
import com.pushkar.ecommerce.productservice.model.dto.UpdateProductRequest;
import com.pushkar.ecommerce.productservice.model.entity.Product;
import com.pushkar.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final Duration PRODUCT_TTL = Duration.ofMinutes(10);
    private static final Duration LIST_TTL    = Duration.ofMinutes(5);
    private static final String   LIST_KEY    = "products:all";

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // ── Read ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        String cached = redisTemplate.opsForValue().get(LIST_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize product list cache; refreshing", e);
                redisTemplate.delete(LIST_KEY);
            }
        }
        List<ProductResponse> products = productRepository.findAll()
                .stream().map(this::toResponse).toList();
        cacheList(products);
        return products;
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        String key = productKey(id);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, ProductResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize product cache for id={}; refreshing", id, e);
                redisTemplate.delete(key);
            }
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        ProductResponse response = toResponse(product);
        cacheProduct(response);
        return response;
    }

    // ── Write ────────────────────────────────────────────────────────────────────

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

        ProductResponse response = toResponse(productRepository.save(product));
        evictListCache();
        cacheProduct(response);
        return response;
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        if (request.name() != null)        product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null)       product.setPrice(request.price());
        if (request.stock() != null)       product.setStock(request.stock());
        if (request.category() != null)    product.setCategory(request.category());
        if (request.active() != null)      product.setActive(request.active());

        ProductResponse response = toResponse(productRepository.save(product));
        evictListCache();
        cacheProduct(response);
        return response;
    }

    @Transactional
    public ProductResponse adjustStock(Long id, int delta) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        int newStock = product.getStock() + delta;
        if (newStock < 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available: " + product.getStock() + ", delta: " + delta);
        }
        product.setStock(newStock);
        ProductResponse response = toResponse(productRepository.save(product));
        evictListCache();
        cacheProduct(response);
        return response;
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setActive(false);      // soft-delete per PRD
        productRepository.save(product);
        redisTemplate.delete(productKey(id));
        evictListCache();
    }

    // ── Cache helpers ────────────────────────────────────────────────────────────

    private void cacheProduct(ProductResponse r) {
        try {
            redisTemplate.opsForValue().set(productKey(r.id()),
                    objectMapper.writeValueAsString(r), PRODUCT_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Could not cache product id={}", r.id(), e);
        }
    }

    private void cacheList(List<ProductResponse> list) {
        try {
            redisTemplate.opsForValue().set(LIST_KEY,
                    objectMapper.writeValueAsString(list), LIST_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Could not cache product list", e);
        }
    }

    private void evictListCache() {
        redisTemplate.delete(LIST_KEY);
    }

    private String productKey(Long id) {
        return "product:" + id;
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getPrice(), p.getStock(), p.getCategory(), p.getActive(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
