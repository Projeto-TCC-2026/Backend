package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DoctorPatientResponse(
        UUID id,
        DoctorSummary doctor,
        PatientSummary patient,
        LocalDateTime createdAt
) {}
