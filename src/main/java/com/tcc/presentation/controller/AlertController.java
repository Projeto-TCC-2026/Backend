package com.tcc.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.service.AlertService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/integration/alerts")
@Tag(name = "Integração de Alertas",
     description = "Avaliação de leituras de sinais vitais — acesso exclusivo de serviço, por chave de integração")
@SecurityRequirement(name = "Integration Key")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAuthority('ROLE_INTEGRATION')")
    @Operation(
        summary = "Avaliar leitura de sinal vital",
        description = "Compara o valor da leitura com a faixa normal cadastrada para o tipo e, quando o valor " +
                      "está fora dessa faixa, cria um alerta com status PENDING. Os limites são inclusivos no " +
                      "normal: valor igual ao mínimo ou ao máximo não gera alerta, e limite nulo significa " +
                      "ausência de limite daquele lado. Quando não há faixa cadastrada para o tipo de leitura, " +
                      "a resposta é de sucesso com alertGenerated=false e nenhum alerta é criado. A leitura em " +
                      "si não é persistida nesta versão. Endpoint chamado por serviço, autenticado por chave de " +
                      "integração no header X-Integration-Key."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Leitura avaliada. alertGenerated indica se um alerta foi criado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dados inválidos na requisição"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chave de integração ausente ou inválida"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Acesso negado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Paciente não encontrado")
    })
    public ResponseEntity<ApiResponse<AlertEvaluationResponse>> evaluateReading(
            @Valid @RequestBody AlertEvaluationRequest request) {

        AlertEvaluationResponse evaluation = alertService.evaluateReading(request);
        ApiResponse<AlertEvaluationResponse> response = ApiResponse.success(evaluation);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
