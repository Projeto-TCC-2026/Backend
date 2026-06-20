package com.tcc.application.dto.response;

import java.time.LocalDate;

public record PatientSummary(
        Long id,
        String fullName,
        LocalDate birthDate
) {}
