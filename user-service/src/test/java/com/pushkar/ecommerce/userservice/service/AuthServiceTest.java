package com.pushkar.ecommerce.userservice.service;

import com.pushkar.ecommerce.userservice.exception.EmailAlreadyExistsException;
import com.pushkar.ecommerce.userservice.exception.InvalidTokenException;
import com.pushkar.ecommerce.userservice.model.dto.AuthResponse;
import com.pushkar.ecommerce.userservice.model.dto.LoginRequest;
import com.pushkar.ecommerce.userservice.model.dto.RefreshRequest;
import com.pushkar.ecommerce.userservice.model.dto.RegisterRequest;
import com.pushkar.ecommerce.userservice.model.entity.User;
import com.pushkar.ecommerce.userservice.repository.UserRepository;
import com.pushkar.ecommerce.userservice.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private Claims claims;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashed-password");
        testUser.setRole("ROLE_USER");

        // Use reflection to set id since UUID generation happens in DB
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, UUID.randomUUID());
        } catch (Exception ignored) {}
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse response = authService.register(new RegisterRequest("test@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailAlreadyExists_throws() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("test@example.com", "password123")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("test@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void login_wrongPassword_throws() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_userNotFound_throws() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "pass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logout_addsTokenToBlocklist() {
        String token = "valid-jwt";
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getClaims(token)).thenReturn(claims);
        when(claims.getId()).thenReturn("jti-123");
        when(jwtTokenProvider.getRemainingExpiryMs(claims)).thenReturn(60_000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        authService.logout("Bearer " + token);

        verify(valueOps).set(eq("token:blocklist:jti-123"), eq("true"), any());
    }

    @Test
    void logout_invalidToken_throws() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.logout("Bearer invalid-token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_withNonRefreshToken_throws() {
        String token = "access-token";
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getClaims(token)).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("ACCESS");

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(token)))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Not a refresh token");
    }
}
