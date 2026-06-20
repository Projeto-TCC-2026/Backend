package com.tcc.application.dto.response;

public record DoctorSummary(
        Long id,
        String fullName,
        String crm,
        String specialty
) {}
