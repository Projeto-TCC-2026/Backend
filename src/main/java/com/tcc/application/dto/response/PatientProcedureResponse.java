package com.tcc.application.dto.response;

import java.time.LocalDate;

public record PatientProcedureResponse(
        Long id,
        PatientSummary patient,
        ProcedureSummary procedure,
        DoctorSummary doctor,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String notes
) {}
