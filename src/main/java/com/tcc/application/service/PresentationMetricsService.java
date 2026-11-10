package com.tcc.application.service;

import com.tcc.application.dto.response.DailyMetricResponse;
import com.tcc.application.dto.response.ProcedureMetricsResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PresentationMetricsService {

    List<DailyMetricResponse> getDailyCheckinSubmissions(String email, LocalDate startDate, LocalDate endDate);

    List<DailyMetricResponse> getDailyPatientsWithReadings(String email, LocalDate startDate, LocalDate endDate);

    List<DailyMetricResponse> getDailyAlerts(String email, LocalDate startDate, LocalDate endDate);

    ProcedureMetricsResponse getProcedureMetrics(String email, UUID procedureId,
            LocalDate startDate, LocalDate endDate);
}
