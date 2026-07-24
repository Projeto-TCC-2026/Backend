package com.tcc.presentation.controller;

import com.tcc.application.dto.response.AdminDashboardResponse;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.HospitalDashboardResponse;
import com.tcc.application.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Métricas retornadas com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Não autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado")
    })
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Métricas retornadas com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Não autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Hospital não encontrado")
    })
    public ResponseEntity<ApiResponse<HospitalDashboardResponse>> getHospitalDashboard(
            @Parameter(description = "ID do hospital", example = "1", required = true)
            @PathVariable Long hospitalId,
            Authentication authentication) {
        
        HospitalDashboardResponse dashboard = dashboardService.getHospitalDashboard(hospitalId, authentication.getName());
        ApiResponse<HospitalDashboardResponse> response = ApiResponse.success(dashboard);
        
        return ResponseEntity.ok(response);
    }
}
