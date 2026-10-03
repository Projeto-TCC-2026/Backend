package com.tcc.application.dto.response;

import java.util.UUID;

/**
 * Veredito da avaliação de uma leitura de sinal vital.
 *
 * <p>{@code healthReadingId} identifica a leitura gravada. Quando a leitura já
 * havia sido recebida antes, {@code duplicateReading} vem true e o id é o da
 * leitura original — nada foi gravado de novo e nenhum alerta foi criado.
 *
 * <p>Quando a leitura está dentro da faixa normal, quando não há faixa cadastrada
 * para o tipo, ou quando já existe alerta em aberto do mesmo paciente para o mesmo
 * tipo, {@code alertGenerated} vem false e severity, alertId e reason vêm nulos.
 * Nesses casos a leitura continua sendo gravada.
 */
public record AlertEvaluationResponse(
        boolean alertGenerated,
        String severity,
        UUID alertId,
        String reason,
        UUID healthReadingId,
        boolean duplicateReading
) {

    /** Leitura já recebida antes: nada gravado, nada criado, nada avisado. */
    public static AlertEvaluationResponse duplicate(UUID healthReadingId) {
        return new AlertEvaluationResponse(false, null, null, null, healthReadingId, true);
    }

    /** Leitura gravada e dentro do esperado, ou sem alerta por deduplicação. */
    public static AlertEvaluationResponse withoutAlert(UUID healthReadingId) {
        return new AlertEvaluationResponse(false, null, null, null, healthReadingId, false);
    }

    /** Leitura gravada e alerta criado a partir dela. */
    public static AlertEvaluationResponse withAlert(String severity, UUID alertId,
                                                   String reason, UUID healthReadingId) {
        return new AlertEvaluationResponse(true, severity, alertId, reason, healthReadingId, false);
    }
}
