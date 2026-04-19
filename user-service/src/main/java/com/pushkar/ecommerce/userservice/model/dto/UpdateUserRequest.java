package com.pushkar.ecommerce.userservice.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email String email,
        @Size(min = 8) String password   // null = no change
) {}
