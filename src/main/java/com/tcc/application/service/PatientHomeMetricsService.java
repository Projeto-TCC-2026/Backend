package com.tcc.application.service;

import com.tcc.application.dto.response.PatientHomeMetricsResponse;
import java.time.LocalDate;
import java.util.UUID;

public interface PatientHomeMetricsService {
    PatientHomeMetricsResponse getHomeMetrics(
            String email, UUID patientProcedureId, LocalDate startDate, LocalDate endDate);
}
