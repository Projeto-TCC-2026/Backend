package com.tcc.application.dto.response;

public record PatientsByHospitalResponse(
        Long hospitalId,
        String hospitalName,
        Long totalPatients
) {}
