package com.tcc.application.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record PatientSummary(
        UUID id,
        String fullName,
        LocalDate birthDate
) {}
