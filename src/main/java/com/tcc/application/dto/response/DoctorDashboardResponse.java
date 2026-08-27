package com.tcc.application.dto.response;

import java.util.UUID;

public record DoctorDashboardResponse(
        UUID doctorId,
        String doctorName,
        String hospitalName,
        Long totalPatients,
        Long activePatients,
        Long patientsWithAlert,
        Long proceduresExecuted,
        Long newPatientsLast30Days
) {}
