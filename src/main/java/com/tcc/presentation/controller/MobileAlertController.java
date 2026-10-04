package com.tcc.presentation.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.request.PatientAlertResponseRequest;
import com.tcc.application.dto.response.AlertResponse;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.PatientAlertAnswerResponse;
import com.tcc.application.service.AlertResponseWindowClosedException;
import com.tcc.application.service.AlertService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/alerts")
@Tag(name = "Alertas do Paciente", description = "Alertas do paciente autenticado no aplicativo")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('PATIENT')")
public class MobileAlertController {

    private final AlertService alertService;

    public MobileAlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AlertResponse>>> listRecentAlerts(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return ResponseEntity.ok(ApiResponse.success(alertService.listRecentForPatient(email, pageable)));
    }

    @PostMapping("/{alertId}/response")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
        summary = "Responder se está bem, após uma medição com valor grave",
        description = "Registra a resposta do paciente à pergunta disparada por uma leitura de valor grave. "
                    + "Só alcança alerta do próprio paciente autenticado: o escopo é verificado por comparação "
                    + "de paciente, não apenas pela role. "
                    + "answer=NOT_OK leva o alerta a PENDING, registra confirmed_at com o horário da medição "
                    + "grave e avisa os médicos vinculados por e-mail na hora. "
                    + "answer=OK leva o alerta a UNCONFIRMED e devolve o caso ao fluxo comum: nenhum médico é "
                    + "avisado agora, e a próxima leitura fora da faixa em até 2 horas confirma o alerta por "
                    + "duas leituras. "
                    + "O prazo de resposta é de 10 minutos a partir da criação do alerta. Vencido o prazo sem "
                    + "resposta, o alerta passa a PENDING automaticamente e os médicos são avisados — a "
                    + "resposta deixa de ser aceita e passa a responder 409. Alerta que já tem resposta "
                    + "registrada também responde 409."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Resposta registrada. alertStatus traz o status resultante e doctorNotified "
                            + "indica se o aviso ao médico foi disparado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Resposta ausente ou diferente de OK e NOT_OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Token ausente, inválido ou expirado, ou alerta de outro paciente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Acesso negado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Alerta não encontrado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Alerta não está mais aguardando resposta: o prazo venceu e o médico já foi "
                            + "avisado, ou a resposta já havia sido registrada")
    })
    public ResponseEntity<?> respondToAlert(
            Authentication authentication,
            @PathVariable UUID alertId,
            @Valid @RequestBody PatientAlertResponseRequest request) {

        String email = ((UserDetails) authentication.getPrincipal()).getUsername();

        try {
            PatientAlertAnswerResponse answer =
                    alertService.registerPatientResponse(email, alertId, request.answer());

            return ResponseEntity.ok(ApiResponse.success(answer));

        } catch (AlertResponseWindowClosedException e) {
            return conflict(e);
        }
    }

    /**
     * Janela de resposta fechada: prazo vencido ou resposta já registrada.
     *
     * <p>Tratado aqui, no controller, e não no {@code GlobalExceptionHandler}: o
     * handler não tem mapeamento para 409 e alterá-lo mudaria o contrato de erro de
     * todos os endpoints. O formato do corpo é o mesmo que o handler produz, para o
     * app não precisar de um segundo formato de erro.
     */
    private ResponseEntity<Map<String, Object>> conflict(AlertResponseWindowClosedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", java.time.LocalDateTime.now(),
                "status", HttpStatus.CONFLICT.value(),
                "error", HttpStatus.CONFLICT.getReasonPhrase(),
                "message", e.getMessage()));
    }
}
