package com.tcc.application.dto.response;

import java.util.UUID;

public record PatientsByHospitalResponse(
        UUID hospitalId,
        String hospitalName,
        Long totalPatients
) {}
