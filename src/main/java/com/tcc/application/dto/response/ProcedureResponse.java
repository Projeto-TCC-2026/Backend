package com.tcc.application.dto.response;

import java.time.LocalDateTime;

public record ProcedureResponse(
        Long id,
        DoctorSummary doctor,
        String title,
        String description,
        Integer estimatedDuration,
        Boolean active,
        LocalDateTime createdAt
) {}
