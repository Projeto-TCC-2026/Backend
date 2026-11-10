package com.tcc.application.dto.response;

import java.time.LocalDate;

public record DailyMetricResponse(
    LocalDate date,
    long count) {
}
