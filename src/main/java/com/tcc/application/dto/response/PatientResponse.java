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
        String email,
        String address,
        String city,
        String state,
        String zipCode,
        String bloodType,
        Double weight,
        Double height,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
