package com.tcc.presentation.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.response.AlertResponse;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.DoctorAlertResponse;
import com.tcc.application.service.AlertService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @GetMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Listar alertas dos pacientes vinculados",
        description = "Devolve os alertas dos pacientes vinculados ao médico autenticado, dos mais recentes "
                    + "aos mais antigos. O médico vem do token: não há parâmetro de médico, e o escopo por "
                    + "paciente é aplicado na consulta pelo vínculo em doctor_patients, não apenas pela role. "
                    + "Status visíveis: PENDING (precisa ser tratado), AWAITING_PATIENT (leitura grave em "
                    + "curso, aguardando a paciente responder) e RESOLVED (já tratado). UNCONFIRMED e "
                    + "NOT_CONFIRMED nunca aparecem: são etapas internas do fluxo de confirmação, e leitura "
                    + "isolada que não se confirmou não é levada ao médico. "
                    + "O parâmetro status é opcional e aceita apenas um dos três visíveis; qualquer outro "
                    + "valor resulta em 400. Sem o parâmetro, os três vêm juntos. "
                    + "Ciclo de status do fluxo comum: UNCONFIRMED vira PENDING quando uma segunda leitura "
                    + "seguida confirma o desvio em até 2h, ou NOT_CONFIRMED quando a leitura seguinte volta "
                    + "ao normal. Ciclo do valor grave: AWAITING_PATIENT vira PENDING quando a paciente "
                    + "responde que não está bem ou quando o prazo de 10 minutos vence sem resposta, e vira "
                    + "UNCONFIRMED quando ela responde que está bem. PENDING vira RESOLVED pelo médico. "
                    + "confirmationReason diz qual evidência confirmou o alerta — DUAS_LEITURAS, "
                    + "PACIENTE_NAO_ESTA_BEM ou SEM_RESPOSTA. Vem nulo em alerta AWAITING_PATIENT, que ainda "
                    + "não foi confirmado, e em alerta confirmado antes da V37, que não tem o motivo gravado. "
                    + "measuredAt e confirmedAt saem em UTC, com o sufixo Z."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Página de alertas dos pacientes vinculados ao médico"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Parâmetro status fora dos valores aceitos"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Token ausente, inválido ou expirado, ou perfil de médico não encontrado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Acesso negado")
    })
    public ResponseEntity<ApiResponse<Page<DoctorAlertResponse>>> listAlerts(
            Authentication authentication,

            @Parameter(description = "Filtra por um único status. Omitido, traz os três visíveis",
                       schema = @Schema(allowableValues = {"PENDING", "AWAITING_PATIENT", "RESOLVED"}))
            @RequestParam(required = false) String status,

            @PageableDefault(size = 20) Pageable pageable) {

        String email = ((UserDetails) authentication.getPrincipal()).getUsername();

        return ResponseEntity.ok(ApiResponse.success(
                alertService.listForDoctor(email, status, pageable)));
    }

    @PatchMapping("/{alertId}/resolve")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Marcar alerta como resolvido",
        description = "Altera o status do alerta para RESOLVED. Só alcança alerta de paciente vinculado ao "
                    + "médico autenticado: o vínculo é verificado na consulta, não apenas pela role. "
                    + "Alerta RESOLVED deixa de contar nos indicadores de alertas pendentes e deixa de bloquear "
                    + "a criação de um novo alerta para o mesmo paciente e tipo de leitura. "
                    + "Ciclo de status atual: no fluxo comum, UNCONFIRMED vira PENDING quando uma segunda "
                    + "leitura seguida confirma o desvio em até 2h, ou NOT_CONFIRMED quando a leitura seguinte "
                    + "volta ao normal. No fluxo de valor grave, AWAITING_PATIENT vira PENDING quando a "
                    + "paciente responde que não está bem ou quando o prazo de 10 minutos vence sem resposta, "
                    + "e vira UNCONFIRMED quando ela responde que está bem. Só PENDING chega a RESOLVED por "
                    + "este endpoint; alerta já RESOLVED é devolvido como está, sem nova gravação."
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
