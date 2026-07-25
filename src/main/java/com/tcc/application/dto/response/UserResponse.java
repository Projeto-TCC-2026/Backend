package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
