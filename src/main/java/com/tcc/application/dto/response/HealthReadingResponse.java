package com.tcc.application.dto.response;

import java.time.LocalDateTime;

public record HealthReadingResponse(
        Long id,
        PatientSummary patient,
        Long patientDeviceId,
        Long readingImportId,
        String readingType,
        String value,
        String unit,
        LocalDateTime measuredAt
) {}
