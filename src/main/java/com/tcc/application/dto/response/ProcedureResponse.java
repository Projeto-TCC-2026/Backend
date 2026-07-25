package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProcedureResponse(
        UUID id,
        DoctorSummary doctor,
        String title,
        String description,
        Integer estimatedDuration,
        Boolean active,
        LocalDateTime createdAt
) {}
