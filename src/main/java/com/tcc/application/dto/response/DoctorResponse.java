package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DoctorResponse(
        UUID id,
        UserResponse user,
        HospitalSummary hospital,
        String fullName,
        String cpf,
        String crm,
        String specialty,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
