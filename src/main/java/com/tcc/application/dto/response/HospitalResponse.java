package com.tcc.application.dto.response;

import java.time.LocalDateTime;

public record HospitalResponse(
        Long id,
        String name,
        String cnpj,
        String phone,
        String email,
        String address,
        String city,
        String state,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
