package com.tcc.application.dto.response;

import java.util.UUID;

/**
 * Veredito da avaliação de uma leitura de sinal vital.
 *
 * <p>{@code healthReadingId} identifica a leitura gravada. Quando a leitura já
 * havia sido recebida antes, {@code duplicateReading} vem true e o id é o da
 * leitura original — nada foi gravado de novo e nenhum alerta foi criado.
 *
 * <p>{@code suspectReading} vem true quando o valor está fora da faixa plausível do
 * tipo: a leitura é gravada como suspeita e não avaliada, então não há alerta nem
 * aviso. A resposta continua sendo de sucesso.
 *
 * <p>{@code alertStatus} é o status do alerta criado ou atualizado nesta chamada
 * (UNCONFIRMED, AWAITING_PATIENT, PENDING ou NOT_CONFIRMED), ou nulo quando nenhum
 * alerta foi tocado. {@code AWAITING_PATIENT} aparece quando o valor está na faixa
 * grave do tipo: o alerta pergunta à paciente se ela está bem, em vez de esperar a
 * segunda leitura.
 *
 * <p>{@code alertGenerated} indica que esta chamada <strong>criou</strong> um alerta
 * novo. Confirmação de alerta existente não cria: nesse caso {@code alertGenerated}
 * é false, mas {@code alertId} e {@code alertStatus} vêm preenchidos.
 */
public record AlertEvaluationResponse(
        boolean alertGenerated,
        String severity,
        UUID alertId,
        String reason,
        UUID healthReadingId,
        boolean duplicateReading,
        boolean suspectReading,
        String alertStatus
) {

    /** Leitura já recebida antes: nada gravado, nada criado, nada avisado. */
    public static AlertEvaluationResponse duplicate(UUID healthReadingId) {
        return new AlertEvaluationResponse(
                false, null, null, null, healthReadingId, true, false, null);
    }

    /** Valor fora da faixa plausível: gravada como suspeita, sem avaliação. */
    public static AlertEvaluationResponse suspect(UUID healthReadingId, String reason) {
        return new AlertEvaluationResponse(
                false, null, null, reason, healthReadingId, false, true, null);
    }

    /** Leitura gravada e avaliada, sem alerta criado nem atualizado. */
    public static AlertEvaluationResponse withoutAlert(UUID healthReadingId) {
        return new AlertEvaluationResponse(
                false, null, null, null, healthReadingId, false, false, null);
    }

    /**
     * Leitura gravada e alerta criado a partir dela, ainda não confirmado:
     * {@code UNCONFIRMED} no fluxo comum, ou {@code AWAITING_PATIENT} quando o valor
     * é grave e a paciente está sendo perguntada.
     */
    public static AlertEvaluationResponse withAlert(String severity, UUID alertId,
                                                    String reason, UUID healthReadingId,
                                                    String alertStatus) {
        return new AlertEvaluationResponse(
                true, severity, alertId, reason, healthReadingId, false, false, alertStatus);
    }

    /**
     * Alerta que já existia teve o status alterado por esta leitura: confirmado
     * (PENDING) ou descartado (NOT_CONFIRMED). Nenhum alerta novo foi criado.
     */
    public static AlertEvaluationResponse withUpdatedAlert(String severity, UUID alertId,
                                                           String reason, UUID healthReadingId,
                                                           String alertStatus) {
        return new AlertEvaluationResponse(
                false, severity, alertId, reason, healthReadingId, false, false, alertStatus);
    }
}
