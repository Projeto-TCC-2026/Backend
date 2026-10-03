package com.tcc.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.response.AlertResponse;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.service.AlertService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Resolução de alerta pelo médico.
 *
 * <p>Fica sob {@code /api/doctor/**}, que o SecurityConfig já restringe à role
 * DOCTOR, e o escopo por paciente é aplicado na consulta de vínculo dentro do
 * service — role sozinha não basta.
 */
@RestController
@RequestMapping("/api/doctor/alerts")
@Tag(name = "Alertas do Médico", description = "Tratamento de alertas dos pacientes vinculados ao médico")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('DOCTOR')")
public class AlertResolutionController {

    private final AlertService alertService;

    public AlertResolutionController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PatchMapping("/{alertId}/resolve")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Marcar alerta como resolvido",
        description = "Altera o status do alerta para RESOLVED. Só alcança alerta de paciente vinculado ao "
                    + "médico autenticado: o vínculo é verificado na consulta, não apenas pela role. "
                    + "Alerta RESOLVED deixa de contar nos indicadores de alertas pendentes e deixa de bloquear "
                    + "a criação de um novo alerta para o mesmo paciente e tipo de leitura."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Alerta marcado como RESOLVED"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Token ausente, inválido ou expirado, ou paciente não vinculado ao médico"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Acesso negado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Alerta não encontrado")
    })
    public ResponseEntity<ApiResponse<AlertResponse>> resolveAlert(
            Authentication authentication,
            @PathVariable UUID alertId) {

        String email = ((UserDetails) authentication.getPrincipal()).getUsername();

        return ResponseEntity.ok(ApiResponse.success(alertService.resolveAlert(email, alertId)));
    }
}
