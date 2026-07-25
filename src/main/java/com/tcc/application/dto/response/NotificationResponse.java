package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        AlertSummary alert,
        DoctorSummary doctor,
        String message,
        LocalDateTime sentAt,
        LocalDateTime readAt,
        String status
) {}
