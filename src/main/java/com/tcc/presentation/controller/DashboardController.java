package com.tcc.presentation.controller;

import com.tcc.application.dto.response.AdminDashboardResponse;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard Administrativo", description = "Métricas globais da plataforma — acesso exclusivo ADMIN")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Dashboard administrativo",
        description = "Retorna métricas globais da plataforma: total de hospitais, médicos e pacientes. "
            + "Os valores vêm de um cache atualizado periodicamente; se o cache não existir, são calculados no banco."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Métricas retornadas com sucesso"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Não autenticado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getAdminDashboardCached()));
    }
}
