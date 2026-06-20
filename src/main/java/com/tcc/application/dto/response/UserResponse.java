package com.tcc.application.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
