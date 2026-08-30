package com.tcc.application.service;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;

public interface AlertService {

    /**
     * Avalia uma leitura de sinal vital contra a faixa normal cadastrada para o tipo
     * e persiste um alerta quando o valor está fora dessa faixa.
     *
     * <p>Os limites são inclusivos no normal: valor igual ao mínimo ou ao máximo não
     * gera alerta. Limite nulo significa ausência de limite daquele lado.
     *
     * <p>Quando não há faixa cadastrada para o tipo de leitura, nenhum alerta é gerado
     * e nenhuma exceção é lançada.
     */
    AlertEvaluationResponse evaluateReading(AlertEvaluationRequest request);
}
