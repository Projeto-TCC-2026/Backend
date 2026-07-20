package com.tcc.application.dto.response;

public record AdminDashboardResponse(
        Long totalHospitals,
        Long totalDoctors,
        Long totalPatients,
        Long totalProcedures,
        Long activeHospitals,
        Long inactiveHospitals
) {}
