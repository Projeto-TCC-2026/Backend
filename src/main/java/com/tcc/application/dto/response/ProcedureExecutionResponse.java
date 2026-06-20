package com.tcc.application.dto.response;

import java.time.LocalDateTime;

public record ProcedureExecutionResponse(
        Long id,
        Long patientProcedureId,
        ProcedureSummary procedure,
        DoctorSummary doctor,
        PatientSummary patient,
        LocalDateTime executionDate,
        String status,
        String observations
) {}
