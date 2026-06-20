package com.tcc.application.dto.response;

import java.time.LocalDateTime;

public record DoctorPatientResponse(
        Long id,
        DoctorSummary doctor,
        PatientSummary patient,
        LocalDateTime createdAt
) {}
