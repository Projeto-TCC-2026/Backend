package com.tcc.application.dto.response;

public record DoctorsByHospitalResponse(
        Long hospitalId,
        String hospitalName,
        Long totalDoctors
) {}
