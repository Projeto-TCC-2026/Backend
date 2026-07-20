package com.tcc.application.dto.response;

import java.util.List;

public record HospitalDashboardResponse(
        Long hospitalId,
        String hospitalName,
        Long totalDoctors,
        Long totalPatients,
        Long totalProcedures,
        List<ProceduresByPeriodResponse> proceduresByPeriod,
        List<PatientSummary> latestPatients
) {}
