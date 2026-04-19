package com.pushkar.ecommerce.userservice.service;

import com.pushkar.ecommerce.userservice.exception.EmailAlreadyExistsException;
import com.pushkar.ecommerce.userservice.exception.ResourceNotFoundException;
import com.pushkar.ecommerce.userservice.model.dto.UpdateUserRequest;
import com.pushkar.ecommerce.userservice.model.dto.UserResponse;
import com.pushkar.ecommerce.userservice.model.entity.User;
import com.pushkar.ecommerce.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (StringUtils.hasText(request.email()) && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new EmailAlreadyExistsException(request.email());
            }
            user.setEmail(request.email());
        }

        if (StringUtils.hasText(request.password())) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return UserResponse.from(userRepository.save(user));
    }
}
