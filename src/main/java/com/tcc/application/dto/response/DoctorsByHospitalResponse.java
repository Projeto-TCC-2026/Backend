package com.tcc.application.dto.response;

import java.util.UUID;

public record DoctorsByHospitalResponse(
        UUID hospitalId,
        String hospitalName,
        Long totalDoctors
) {}
