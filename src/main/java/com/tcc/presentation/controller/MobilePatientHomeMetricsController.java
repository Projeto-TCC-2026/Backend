package com.tcc.presentation.controller;

import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.PatientHomeMetricsResponse;
import com.tcc.application.service.PatientHomeMetricsService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/patient-procedures")
@PreAuthorize("hasRole('PATIENT')")
public class MobilePatientHomeMetricsController {
    private final PatientHomeMetricsService metricsService;

    public MobilePatientHomeMetricsController(PatientHomeMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/{patientProcedureId}/home-metrics")
    public ResponseEntity<ApiResponse<PatientHomeMetricsResponse>> getHomeMetrics(
            Authentication authentication,
            @PathVariable UUID patientProcedureId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return ResponseEntity.ok(ApiResponse.success(
                metricsService.getHomeMetrics(email, patientProcedureId, startDate, endDate)));
    }
}
