package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record HospitalResponse(
        UUID id,
        String name,
        String cnpj,
        String phone,
        String email,
        String address,
        String city,
        String state,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
