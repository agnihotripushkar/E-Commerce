package com.pushkar.ecommerce.userservice.controller;

import com.pushkar.ecommerce.userservice.model.dto.UpdateUserRequest;
import com.pushkar.ecommerce.userservice.model.dto.UserResponse;
import com.pushkar.ecommerce.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
@SecurityRequirement(name = "Bearer Auth")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id, Authentication auth) {
        requireSelfOrAdmin(id, auth);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateUserRequest request,
                                                    Authentication auth) {
        requireSelfOrAdmin(id, auth);
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    private void requireSelfOrAdmin(UUID targetId, Authentication auth) {
        boolean isSelf = auth.getPrincipal().toString().equals(targetId.toString());
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (!isSelf && !isAdmin) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
