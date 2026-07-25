package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PatientDeviceResponse(
        UUID id,
        PatientSummary patient,
        String deviceIdentifier,
        String deviceType,
        String manufacturer,
        String model,
        Boolean active,
        LocalDateTime createdAt
) {}
