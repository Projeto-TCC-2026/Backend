package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlertSummary(
        UUID id,
        String severity,
        String title,
        String status,
        LocalDateTime createdAt
) {}
