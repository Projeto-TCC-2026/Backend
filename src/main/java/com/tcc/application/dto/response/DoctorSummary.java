package com.tcc.application.dto.response;

import java.util.UUID;

public record DoctorSummary(
        UUID id,
        String fullName,
        String crm,
        String specialty
) {}
