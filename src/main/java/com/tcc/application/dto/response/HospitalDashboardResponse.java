package com.tcc.application.dto.response;

import java.util.List;
import java.util.UUID;

public record HospitalDashboardResponse(
        UUID hospitalId,
        String hospitalName,
        Long totalDoctors,
        Long totalPatients,
        Long totalProcedures,
        List<ProceduresByPeriodResponse> proceduresByPeriod,
        List<PatientSummary> latestPatients
) {}
