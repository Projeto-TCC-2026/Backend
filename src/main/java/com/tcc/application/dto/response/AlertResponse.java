package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        PatientSummary patient,
        UUID healthReadingId,
        String severity,
        String title,
        String description,
        String status,
        LocalDateTime createdAt
) {}
