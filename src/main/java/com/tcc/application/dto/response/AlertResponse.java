package com.tcc.application.dto.response;

import java.time.LocalDateTime;

public record AlertResponse(
        Long id,
        PatientSummary patient,
        Long healthReadingId,
        String severity,
        String title,
        String description,
        String status,
        LocalDateTime createdAt
) {}
