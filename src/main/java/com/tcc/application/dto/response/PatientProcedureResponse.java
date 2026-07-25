package com.tcc.application.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record PatientProcedureResponse(
        UUID id,
        PatientSummary patient,
        ProcedureSummary procedure,
        DoctorSummary doctor,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String notes
) {}
