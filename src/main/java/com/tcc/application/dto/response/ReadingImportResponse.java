package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReadingImportResponse(
        UUID id,
        PatientSummary patient,
        UUID patientDeviceId,
        String sourceFile,
        LocalDateTime importedAt
) {}
