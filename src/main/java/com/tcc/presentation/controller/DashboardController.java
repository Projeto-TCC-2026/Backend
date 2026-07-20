package com.tcc.presentation.controller;

import com.tcc.application.dto.response.AdminDashboardResponse;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.HospitalDashboardResponse;
import com.tcc.application.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Endpoints de Dashboard para visualização de métricas e estatísticas")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Dashboard Administrativo",
        description = "Retorna métricas gerais do sistema incluindo totais de hospitais, doutores, pacientes e procedimentos. " +
                      "Acesso restrito a administradores."
    )
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard() {
        AdminDashboardResponse dashboard = dashboardService.getAdminDashboard();
        ApiResponse<AdminDashboardResponse> response = ApiResponse.success(dashboard);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hospital/{hospitalId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Dashboard do Hospital",
        description = "Retorna métricas específicas de um hospital incluindo totais de doutores, pacientes, procedimentos, " +
                      "procedimentos por período e últimos pacientes cadastrados."
    )
    public ResponseEntity<ApiResponse<HospitalDashboardResponse>> getHospitalDashboard(
            @Parameter(description = "ID do hospital", example = "1", required = true)
            @PathVariable Long hospitalId) {
        
        HospitalDashboardResponse dashboard = dashboardService.getHospitalDashboard(hospitalId);
        ApiResponse<HospitalDashboardResponse> response = ApiResponse.success(dashboard);
        
        return ResponseEntity.ok(response);
    }
}
