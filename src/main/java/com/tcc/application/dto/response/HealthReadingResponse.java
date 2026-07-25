package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record HealthReadingResponse(
        UUID id,
        PatientSummary patient,
        UUID patientDeviceId,
        UUID readingImportId,
        String readingType,
        String value,
        String unit,
        LocalDateTime measuredAt
) {}
