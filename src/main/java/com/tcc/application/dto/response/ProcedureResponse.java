package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProcedureResponse(
        UUID id,
        HospitalSummary hospital,
        String title,
        String description,
        Integer estimatedDuration,
        Boolean active,
        LocalDateTime createdAt,
        List<DoctorSummary> doctors
) {}
