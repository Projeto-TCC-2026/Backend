package com.tcc.application.dto.response;

import java.time.LocalDateTime;

public record AlertSummary(
        Long id,
        String severity,
        String title,
        String status,
        LocalDateTime createdAt
) {}
