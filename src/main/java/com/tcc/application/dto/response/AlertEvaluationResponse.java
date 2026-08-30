package com.tcc.application.dto.response;

import java.util.UUID;

/**
 * Veredito da avaliação de uma leitura de sinal vital.
 *
 * Quando a leitura está dentro da faixa normal, ou quando não há faixa cadastrada
 * para o tipo de leitura, severity, alertId e reason vêm nulos.
 */
public record AlertEvaluationResponse(
        boolean alertGenerated,
        String severity,
        UUID alertId,
        String reason
) {}
