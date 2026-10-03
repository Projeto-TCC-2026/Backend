package com.tcc.application.service;

import java.util.UUID;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;
import com.tcc.application.dto.response.AlertResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AlertService {

    /**
     * Grava a leitura de sinal vital recebida, avalia-a contra a faixa normal
     * cadastrada para o tipo e persiste um alerta quando o valor está fora dessa
     * faixa.
     *
     * <p>Toda leitura é gravada, normal ou não, e o alerta criado aponta para ela.
     *
     * <p>Idempotência: leitura com o mesmo paciente, tipo e horário de medição já
     * gravada não é gravada de novo, não gera alerta e não dispara aviso. A resposta
     * é de sucesso, com {@code duplicateReading=true}.
     *
     * <p>Deduplicação: se já existe alerta PENDING do mesmo paciente para o mesmo
     * tipo de leitura, nenhum novo alerta é criado e nenhum aviso é disparado. A
     * leitura continua sendo gravada.
     *
     * <p>Os limites são inclusivos no normal: valor igual ao mínimo ou ao máximo não
     * gera alerta. Limite nulo significa ausência de limite daquele lado.
     *
     * <p>Quando não há faixa cadastrada para o tipo de leitura, nenhum alerta é
     * gerado e nenhuma exceção é lançada — a leitura ainda assim é gravada.
     *
     * @throws AlertDuplicateReadingException quando a leitura é barrada pela
     *         restrição de unicidade do banco, em corrida com uma requisição idêntica
     */
    AlertEvaluationResponse evaluateReading(AlertEvaluationRequest request);

    Page<AlertResponse> listRecentForPatient(String email, Pageable pageable);

    /**
     * Marca um alerta como RESOLVED. Restrito a alerta de paciente vinculado ao
     * médico autenticado.
     */
    AlertResponse resolveAlert(String email, UUID alertId);
}
