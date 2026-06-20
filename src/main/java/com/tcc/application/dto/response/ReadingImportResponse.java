package com.tcc.application.dto.response;

import java.time.LocalDateTime;

public record ReadingImportResponse(
        Long id,
        PatientSummary patient,
        Long patientDeviceId,
        String sourceFile,
        LocalDateTime importedAt
) {}
