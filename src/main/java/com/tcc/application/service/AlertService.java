package com.tcc.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;
import com.tcc.application.dto.response.AlertResponse;

public interface AlertService {

    /**
     * Grava a leitura de sinal vital recebida e decide o que fazer com ela.
     *
     * <p>Toda leitura é gravada, normal ou não, e o alerta criado aponta para ela.
     *
     * <p><strong>Valor impossível:</strong> leitura fora da faixa plausível do tipo é
     * gravada com {@code suspect = true}, não é avaliada, não gera alerta e não
     * dispara aviso. Tipo sem faixa plausível cadastrada não sofre o filtro.
     *
     * <p><strong>Idempotência:</strong> leitura com o mesmo paciente, tipo e horário
     * de medição já gravada não é gravada de novo, não gera alerta e não dispara
     * aviso. A resposta é de sucesso, com {@code duplicateReading=true}.
     *
     * <p><strong>Confirmação por duas leituras:</strong> a primeira leitura fora da
     * faixa normal cria alerta {@code UNCONFIRMED} e avisa apenas o paciente. Se a
     * leitura imediatamente anterior do tipo for a do alerta e a distância entre as
     * duas medições couber em 2 horas, o alerta passa a {@code PENDING}, registra
     * {@code confirmedAt} e só então os médicos são avisados. Leitura dentro da faixa
     * com um {@code UNCONFIRMED} aberto leva o alerta a {@code NOT_CONFIRMED}.
     * Leitura suspeita é ignorada nessa sequência.
     *
     * <p><strong>Janela de 4 horas:</strong> enquanto existir alerta
     * {@code PENDING} do mesmo paciente e tipo confirmado há menos de 4 horas, nova
     * leitura fora da faixa é apenas gravada. Passada a janela, o fluxo recomeça.
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
