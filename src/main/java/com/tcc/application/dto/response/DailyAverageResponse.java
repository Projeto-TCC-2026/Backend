package com.tcc.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyAverageResponse(
    LocalDate date,
    BigDecimal average,
    long sampleSize) {
}
