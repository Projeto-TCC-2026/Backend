package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProcedureExecutionResponse(
        UUID id,
        UUID patientProcedureId,
        ProcedureSummary procedure,
        DoctorSummary doctor,
        PatientSummary patient,
        LocalDateTime executionDate,
        String status,
        String observations
) {}
