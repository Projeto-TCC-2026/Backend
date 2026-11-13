package com.tcc.application.dto.response;

import java.time.LocalDate;

public record DailyCheckinStatusResponse(
        LocalDate date,
        boolean submitted) {
}
