package com.tcc.application.dto.response;

public record AdminDashboardResponse(
        Long totalHospitals,
        Long totalDoctors,
        Long activeDoctors,
        Long inactiveDoctors,
        Long totalPatients
) {}
