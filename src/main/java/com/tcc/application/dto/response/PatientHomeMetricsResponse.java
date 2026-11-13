package com.tcc.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientHomeMetricsResponse(
        UUID patientProcedureId,
        String procedureTitle,
        LocalDate startDate,
        boolean submittedToday,
        List<DailyCheckinStatusResponse> dailyCheckins,
        List<PatientFieldTrendResponse> numericFields) {
}
