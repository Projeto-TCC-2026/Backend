package com.tcc.presentation.controller;

import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.DoctorsByHospitalResponse;
import com.tcc.application.dto.response.PatientsByHospitalResponse;
import com.tcc.application.dto.response.ProceduresByDoctorResponse;
import com.tcc.application.dto.response.ProceduresByPeriodResponse;
import com.tcc.application.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Relatórios", description = "Endpoints para geração de relatórios e análises de dados")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/patients-by-hospital")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Relatório de Pacientes por Hospital",
        description = "Retorna a quantidade de pacientes ativos agrupados por hospital. " +
                      "Os resultados são ordenados por quantidade de pacientes (decrescente). " +
                      "Acesso restrito a administradores."
    )
    public ResponseEntity<ApiResponse<List<PatientsByHospitalResponse>>> getPatientsByHospital() {
        List<PatientsByHospitalResponse> report = reportService.getPatientsByHospital();
        ApiResponse<List<PatientsByHospitalResponse>> response = ApiResponse.success(report);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctors-by-hospital")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Relatório de Doutores por Hospital",
        description = "Retorna a quantidade de doutores agrupados por hospital. " +
                      "Os resultados são ordenados por quantidade de doutores (decrescente). " +
                      "Acesso restrito a administradores."
    )
    public ResponseEntity<ApiResponse<List<DoctorsByHospitalResponse>>> getDoctorsByHospital() {
        List<DoctorsByHospitalResponse> report = reportService.getDoctorsByHospital();
        ApiResponse<List<DoctorsByHospitalResponse>> response = ApiResponse.success(report);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/procedures-by-doctor")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Relatório de Procedimentos por Doutor",
        description = "Retorna a quantidade de procedimentos ativos agrupados por doutor. " +
                      "Inclui informações do doutor (nome, especialidade) e total de procedimentos. " +
                      "Os resultados são ordenados por quantidade de procedimentos (decrescente)."
    )
    public ResponseEntity<ApiResponse<List<ProceduresByDoctorResponse>>> getProceduresByDoctor() {
        List<ProceduresByDoctorResponse> report = reportService.getProceduresByDoctor();
        ApiResponse<List<ProceduresByDoctorResponse>> response = ApiResponse.success(report);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/procedures-by-period")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Relatório de Procedimentos por Período",
        description = "Retorna a quantidade de procedimentos ativos agrupados por período (mês/ano). " +
                      "Requer data de início e fim para o período de análise. " +
                      "Os resultados são ordenados por período (decrescente)."
    )
    public ResponseEntity<ApiResponse<List<ProceduresByPeriodResponse>>> getProceduresByPeriod(
            @Parameter(
                description = "Data de início do período (formato: yyyy-MM-dd'T'HH:mm:ss)",
                example = "2026-01-01T00:00:00",
                required = true
            )
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(
                description = "Data de fim do período (formato: yyyy-MM-dd'T'HH:mm:ss)",
                example = "2026-12-31T23:59:59",
                required = true
            )
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<ProceduresByPeriodResponse> report = reportService.getProceduresByPeriod(startDate, endDate);
        ApiResponse<List<ProceduresByPeriodResponse>> response = ApiResponse.success(report);
        
        return ResponseEntity.ok(response);
    }
}
