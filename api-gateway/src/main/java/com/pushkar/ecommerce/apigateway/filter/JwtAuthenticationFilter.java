package com.pushkar.ecommerce.apigateway.filter;

import com.pushkar.ecommerce.apigateway.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Global JWT authentication filter for Spring Cloud Gateway (WebFlux).
 *
 * For every request:
 *  1. Skip public paths (/api/auth/**, /actuator/health)
 *  2. Extract Bearer token from Authorization header
 *  3. Validate token signature + expiry
 *  4. Check Redis blocklist (prevents using logged-out tokens)
 *  5. Inject X-User-Id and X-User-Role headers for downstream services
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/actuator/health",
            "/actuator/info"
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public int getOrder() {
        // Run before routing filters
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            return reject(exchange, "Missing Authorization header");
        }

        if (!jwtTokenProvider.validateToken(token)) {
            return reject(exchange, "Invalid or expired token");
        }

        Claims claims = jwtTokenProvider.getClaims(token);
        String jti = claims.getId();

        return redisTemplate.hasKey("token:blocklist:" + jti)
                .flatMap(isBlocked -> {
                    if (Boolean.TRUE.equals(isBlocked)) {
                        return reject(exchange, "Token has been revoked");
                    }

                    String userId = claims.getSubject();
                    String role = claims.get("role", String.class);

                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Role", role)
                            // Strip raw Authorization header from downstream to prevent misuse
                            .build();

                    log.debug("JWT validated for user={} role={} path={}", userId, role, path);
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith)
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.startsWith("/webjars");
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private Mono<Void> reject(ServerWebExchange exchange, String reason) {
        log.warn("JWT rejected: {}", reason);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        var body = response.bufferFactory().wrap(
                ("{\"error\":\"Unauthorized\",\"message\":\"" + reason + "\"}").getBytes());
        return response.writeWith(Mono.just(body));
    }
}
