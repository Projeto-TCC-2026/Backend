package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DoctorProcedureResponse(
        UUID id,
        DoctorSummary doctor,
        ProcedureSummary procedure,
        LocalDateTime createdAt
) {}
