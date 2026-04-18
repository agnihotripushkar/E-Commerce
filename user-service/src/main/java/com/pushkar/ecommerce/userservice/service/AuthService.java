package com.pushkar.ecommerce.userservice.service;

import com.pushkar.ecommerce.userservice.exception.EmailAlreadyExistsException;
import com.pushkar.ecommerce.userservice.exception.InvalidTokenException;
import com.pushkar.ecommerce.userservice.exception.ResourceNotFoundException;
import com.pushkar.ecommerce.userservice.model.dto.AuthResponse;
import com.pushkar.ecommerce.userservice.model.dto.LoginRequest;
import com.pushkar.ecommerce.userservice.model.dto.RefreshRequest;
import com.pushkar.ecommerce.userservice.model.dto.RegisterRequest;
import com.pushkar.ecommerce.userservice.model.entity.User;
import com.pushkar.ecommerce.userservice.repository.UserRepository;
import com.pushkar.ecommerce.userservice.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("ROLE_USER");
        user = userRepository.save(user);

        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        Claims claims = jwtTokenProvider.getClaims(refreshToken);

        if (!"REFRESH".equals(claims.get("type", String.class))) {
            throw new InvalidTokenException("Not a refresh token");
        }

        String jti = claims.getId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey("token:blocklist:" + jti))) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Revoke the old refresh token so it cannot be reused
        long remainingMs = jwtTokenProvider.getRemainingExpiryMs(claims);
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(
                    "token:blocklist:" + jti, "true", Duration.ofMillis(remainingMs));
        }

        return issueTokens(user);
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException("Missing or malformed Authorization header");
        }
        String accessToken = authorizationHeader.substring(7);

        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new InvalidTokenException("Invalid access token");
        }

        Claims claims = jwtTokenProvider.getClaims(accessToken);
        long remainingMs = jwtTokenProvider.getRemainingExpiryMs(claims);
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(
                    "token:blocklist:" + claims.getId(), "true", Duration.ofMillis(remainingMs));
        }
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        return new AuthResponse(accessToken, refreshToken, "Bearer", 900L);
    }
}
