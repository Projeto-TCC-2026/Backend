package com.tcc.application.dto.response;

import java.time.LocalDateTime;

public record PatientDeviceResponse(
        Long id,
        PatientSummary patient,
        String deviceIdentifier,
        String deviceType,
        String manufacturer,
        String model,
        Boolean active,
        LocalDateTime createdAt
) {}
