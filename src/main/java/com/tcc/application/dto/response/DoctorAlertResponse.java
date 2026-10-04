package com.tcc.application.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.tcc.domain.event.AlertConfirmationReason;
import com.tcc.domain.model.PatientAlertAnswer;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Alerta de um paciente vinculado, como o médico vê na listagem.
 *
 * <p>Diferente de {@link AlertResponse}, que é o alerta do ponto de vista do
 * paciente, este formato traz o que o médico precisa para triar: quem é o
 * paciente, qual foi a medição, e por que o alerta chegou até ele.
 *
 * <p>O paciente entra achatado — {@code patientId} e {@code patientName} — em vez
 * de um {@code PatientSummary} aninhado. O resumo do paciente carrega CPF, e-mail
 * e telefone, que são dado de identificação e não têm uso em uma lista de triagem
 * de alertas.
 *
 * <p>{@code measuredAt} e {@code confirmedAt} são {@link OffsetDateTime} em UTC,
 * então serializam com o sufixo {@code Z} e descrevem um instante sem
 * ambiguidade. As colunas guardam UTC em {@code TIMESTAMP} sem fuso, e um
 * {@code LocalDateTime} no JSON sairia sem fuso — deixando o cliente adivinhar se
 * {@code 14:30} é local ou UTC. A conversão acontece na borda, no mapper: nenhuma
 * coluna e nenhum tipo de entidade muda por causa disso.
 */
@Schema(description = "Alerta de paciente vinculado, na visão do médico")
public record DoctorAlertResponse(

        @Schema(description = "ID do alerta")
        UUID id,

        @Schema(description = "ID do paciente")
        UUID patientId,

        @Schema(description = "Nome do paciente")
        String patientName,

        @Schema(description = "Tipo da leitura que originou o alerta",
                example = "HEART_RATE")
        String readingType,

        @Schema(description = "Valor medido, como recebido do dispositivo",
                example = "38.0")
        String readingValue,

        @Schema(description = "Unidade da medição", example = "bpm")
        String unit,

        @Schema(description = "Horário da medição, em UTC",
                example = "2026-08-29T14:40:00Z")
        OffsetDateTime measuredAt,

        @Schema(description = "Severidade da faixa que originou o alerta",
                example = "CRITICAL")
        String severity,

        @Schema(description = "Rótulo do alerta")
        String title,

        @Schema(description = "Status atual do alerta",
                allowableValues = {"PENDING", "AWAITING_PATIENT", "RESOLVED"})
        String status,

        @Schema(description = "Horário em que o alerta foi confirmado, em UTC. "
                            + "Nulo enquanto o alerta não foi confirmado",
                example = "2026-08-29T14:40:00Z")
        OffsetDateTime confirmedAt,

        @Schema(description = "Resposta da paciente à pergunta de leitura grave. "
                            + "Nulo quando ela não foi perguntada ou não respondeu")
        PatientAlertAnswer patientResponse,

        @Schema(description = "Motivo da confirmação do alerta. Nulo em alerta ainda "
                            + "aguardando a paciente e em alerta confirmado antes da V37, "
                            + "que não tem o motivo registrado")
        AlertConfirmationReason confirmationReason
) {
}
