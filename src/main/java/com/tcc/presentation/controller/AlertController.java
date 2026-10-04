package com.tcc.presentation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.tcc.application.service.AlertDuplicateReadingException;
import com.tcc.application.service.AlertService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/integration/alerts")
@Tag(name = "Integração de Alertas",
     description = "Recebimento e avaliação de leituras de sinais vitais — acesso exclusivo de serviço, "
                 + "por chave de integração")
@SecurityRequirement(name = "Integration Key")
public class AlertController {

    private static final Logger log = LoggerFactory.getLogger(AlertController.class);

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAuthority('ROLE_INTEGRATION')")
    @Operation(
        summary = "Receber e avaliar leitura de sinal vital",
        description = "Grava a leitura recebida em health_readings e decide o que fazer com ela. "
                    + "Valor impossível: quando o valor está fora da faixa plausível do tipo, a leitura é "
                    + "gravada com suspect=true, não é avaliada, não gera alerta e não avisa ninguém — a "
                    + "resposta é 201 com suspectReading=true. Tipo sem faixa plausível cadastrada não sofre "
                    + "esse filtro. "
                    + "Avaliação: o valor é comparado com a faixa normal do tipo. Os limites são inclusivos no "
                    + "normal, e limite nulo significa ausência de limite daquele lado. Quando não há faixa "
                    + "cadastrada para o tipo, a leitura é gravada e a resposta vem com alertGenerated=false. "
                    + "Valor grave: leitura plausível dentro da faixa grave do tipo não espera a segunda "
                    + "leitura. O alerta é criado com status AWAITING_PATIENT e prazo de 10 minutos, e a "
                    + "paciente recebe um push perguntando se está bem; o médico ainda não é avisado. "
                    + "Respondendo que não está bem, o alerta passa a PENDING e o médico é avisado na hora; "
                    + "respondendo que está bem, passa a UNCONFIRMED e volta ao fluxo comum. Sem resposta em "
                    + "10 minutos, um processo em segundo plano leva o alerta a PENDING e avisa o médico. Já "
                    + "existindo alerta AWAITING_PATIENT do mesmo paciente e tipo, a leitura é apenas gravada. "
                    + "Os limites graves são inclusivos por dentro: é grave quando o valor é menor ou igual ao "
                    + "mínimo grave, ou maior ou igual ao máximo grave. Tipo sem faixa grave cadastrada segue "
                    + "apenas o fluxo comum. "
                    + "Confirmação por duas leituras: aplica-se à leitura fora da faixa que não é grave. A "
                    + "primeira leitura fora da faixa cria um alerta "
                    + "UNCONFIRMED e dispara apenas o push ao paciente. Se a leitura seguinte do mesmo tipo "
                    + "também estiver fora da faixa e tiver sido medida em até 2 horas depois, o alerta passa a "
                    + "PENDING, grava confirmed_at e só então os médicos vinculados recebem e-mail, com as duas "
                    + "leituras. Se a leitura seguinte estiver dentro da faixa, o alerta passa a NOT_CONFIRMED e "
                    + "ninguém é avisado. Leitura suspeita no meio é ignorada: não confirma nem quebra a "
                    + "sequência. "
                    + "Janela de 4 horas: enquanto existir alerta PENDING do mesmo paciente e tipo confirmado há "
                    + "menos de 4 horas, nova leitura fora da faixa é apenas gravada, sem criar alerta e sem "
                    + "avisar. Passada a janela, uma nova leitura fora da faixa cria outro UNCONFIRMED. "
                    + "Idempotência: leitura com o mesmo patientId, readingType e measuredAt já recebida não é "
                    + "gravada de novo, não gera alerta e não avisa ninguém — a resposta é 200 com "
                    + "duplicateReading=true e o id da leitura original. "
                    + "Endpoint chamado por serviço, autenticado por chave de integração no header "
                    + "X-Integration-Key."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Leitura já recebida anteriormente. Nada foi gravado e nenhum aviso foi enviado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Leitura gravada. alertGenerated indica se um alerta foi criado, alertStatus traz "
                            + "o status do alerta criado ou atualizado, e suspectReading indica leitura "
                            + "descartada por valor implausível"),
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
                description = "Paciente não encontrado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Erro inesperado no processamento da leitura")
    })
    public ResponseEntity<ApiResponse<AlertEvaluationResponse>> evaluateReading(
            @Valid @RequestBody AlertEvaluationRequest request) {

        AlertEvaluationResponse evaluation = evaluate(request);

        // Leitura repetida é sucesso, não erro: quem chama é uma fila com
        // retentativa, e um 4xx/5xx aqui faria a mensagem ser reentregue para
        // sempre. 200 em vez de 201 porque nada foi criado nesta chamada.
        HttpStatus status = evaluation.duplicateReading() ? HttpStatus.OK : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(ApiResponse.success(evaluation));
    }

    /**
     * Traduz a corrida de leitura duplicada em resposta de sucesso.
     *
     * <p>Duas requisições idênticas em paralelo passam as duas pela checagem de
     * idempotência do service; a restrição de unicidade do banco separa as duas e a
     * perdedora chega aqui. O resultado observável para quem chama é o mesmo de uma
     * leitura repetida detectada por consulta: 200, nada gravado, ninguém avisado.
     *
     * <p>O id da leitura vencedora não é devolvido neste caminho: a transação que o
     * conheceria sofreu rollback. O campo vem nulo, e {@code duplicateReading}
     * continua sendo o sinal confiável.
     *
     * <p>Tratado localmente, no controller, em vez de no handler global: para
     * qualquer outro chamador uma violação de integridade é erro, e só este endpoint
     * tem o contexto para considerá-la sucesso.
     */
    private AlertEvaluationResponse evaluate(AlertEvaluationRequest request) {
        try {
            return alertService.evaluateReading(request);
        } catch (AlertDuplicateReadingException e) {
            log.info("Leitura concorrente repetida do paciente {}. Respondendo sucesso.",
                    request.patientId());
            return AlertEvaluationResponse.duplicate(null);
        }
    }
}
