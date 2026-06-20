package com.tcc.application.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        AlertSummary alert,
        DoctorSummary doctor,
        String message,
        LocalDateTime sentAt,
        LocalDateTime readAt,
        String status
) {}
