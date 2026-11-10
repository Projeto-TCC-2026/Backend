package com.tcc.presentation.controller;

import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.DailyMetricResponse;
import com.tcc.application.dto.response.ProcedureMetricsResponse;
import com.tcc.application.service.PresentationMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
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
@RequestMapping("/api/presentation/metrics")
@PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
@Tag(name = "Métricas para apresentação", description = "Séries diárias para gráficos de médicos, hospitais e administradores")
@SecurityRequirement(name = "******")
public class PresentationMetricsController {

        private final PresentationMetricsService metricsService;

        public PresentationMetricsController(PresentationMetricsService metricsService) {
                this.metricsService = metricsService;
        }

        @GetMapping("/daily-checkins")
        @Operation(summary = "Envios diários de questionários", description = "Retorna o total de envios de questionário em cada dia do período, incluindo dias sem envios.")
        public ResponseEntity<ApiResponse<List<DailyMetricResponse>>> getDailyCheckins(
                        Authentication authentication,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                return ResponseEntity.ok(ApiResponse.success(metricsService.getDailyCheckinSubmissions(
                                extractEmail(authentication), startDate, endDate)));
        }

        @GetMapping("/daily-smartwatch-patients")
        @Operation(summary = "Pacientes com leituras diárias do smartwatch", description = "Conta uma vez por dia cada paciente com leitura de saúde recebida.")
        public ResponseEntity<ApiResponse<List<DailyMetricResponse>>> getDailySmartwatchPatients(
                        Authentication authentication,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                return ResponseEntity.ok(ApiResponse.success(metricsService.getDailyPatientsWithReadings(
                                extractEmail(authentication), startDate, endDate)));
        }

        @GetMapping("/daily-alerts")
        @Operation(summary = "Alertas diários", description = "Retorna alertas encaminhados ao médico (PENDING ou RESOLVED) por dia.")
        public ResponseEntity<ApiResponse<List<DailyMetricResponse>>> getDailyAlerts(
                        Authentication authentication,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                return ResponseEntity.ok(ApiResponse.success(metricsService.getDailyAlerts(
                                extractEmail(authentication), startDate, endDate)));
        }

        @GetMapping("/procedures/{procedureId}")
        @Operation(summary = "Médias diárias dos campos numéricos de um procedimento", description = "A data inicial e final devem delimitar sete dias inclusivos. A resposta também inclui os sete dias anteriores. ADMIN pode consultar qualquer procedimento; HOSPITAL, os procedimentos do próprio hospital; DOCTOR, os procedimentos atribuídos ao próprio médico.")
        public ResponseEntity<ApiResponse<ProcedureMetricsResponse>> getProcedureMetrics(
                        Authentication authentication,
                        @PathVariable UUID procedureId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                return ResponseEntity.ok(ApiResponse.success(metricsService.getProcedureMetrics(
                                extractEmail(authentication), procedureId, startDate, endDate)));
        }

        private String extractEmail(Authentication authentication) {
                return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
}
