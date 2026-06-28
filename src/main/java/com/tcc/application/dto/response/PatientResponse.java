package com.tcc.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientResponse(
        Long id,
        UserResponse user,
        String fullName,
        String cpf,
        LocalDate birthDate,
        String gender,
        String phone,
        Double weight,
        Double height,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
