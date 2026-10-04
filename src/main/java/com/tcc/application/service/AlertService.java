package com.tcc.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tcc.application.dto.request.AlertEvaluationRequest;
import com.tcc.application.dto.response.AlertEvaluationResponse;
import com.tcc.application.dto.response.AlertResponse;
import com.tcc.application.dto.response.PatientAlertAnswerResponse;
import com.tcc.domain.model.PatientAlertAnswer;

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
     * <p><strong>Valor grave:</strong> leitura plausível dentro da faixa grave do
     * tipo não espera a segunda leitura. O alerta nasce {@code AWAITING_PATIENT} com
     * prazo de 10 minutos e a paciente é perguntada se está bem; o médico ainda não é
     * avisado. Já existindo {@code AWAITING_PATIENT} do mesmo paciente e tipo, a
     * leitura é apenas gravada. Tipo sem faixa grave cadastrada segue só o fluxo
     * comum. Os limites graves são inclusivos por dentro: é grave quando o valor é
     * menor ou igual a {@code severeMin}, ou maior ou igual a {@code severeMax}.
     *
     * <p><strong>Janela de 4 horas:</strong> enquanto existir alerta
     * {@code PENDING} do mesmo paciente e tipo confirmado há menos de 4 horas, nova
     * leitura fora da faixa é apenas gravada — grave ou não. Passada a janela, o
     * fluxo recomeça.
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

    /**
     * Registra a resposta da paciente à pergunta disparada por uma leitura grave.
     *
     * <p>Restrito a alerta da própria paciente autenticada: o escopo é verificado por
     * comparação de paciente, não só pela role.
     *
     * <p>{@code NOT_OK} leva o alerta a {@code PENDING}, com {@code confirmedAt}
     * igual ao {@code measuredAt} da leitura grave, e dispara o aviso ao médico.
     * {@code OK} leva a {@code UNCONFIRMED} e devolve o alerta ao fluxo comum: a
     * próxima leitura fora da faixa, em até 2 horas, confirma por duas leituras.
     *
     * @throws com.tcc.exception.ResourceNotFoundException quando o alerta não existe
     * @throws com.tcc.exception.UnauthorizedException quando o alerta é de outra
     *         paciente
     * @throws AlertResponseWindowClosedException quando o alerta não está mais
     *         aguardando resposta: prazo vencido ou resposta já registrada
     */
    PatientAlertAnswerResponse registerPatientResponse(String email, UUID alertId,
                                                       PatientAlertAnswer answer);

    /**
     * Ids dos alertas {@code AWAITING_PATIENT} cujo prazo de resposta já venceu.
     *
     * <p>Consumido pelo agendador, que trata cada id com
     * {@link #confirmAlertWithoutPatientResponse}.
     */
    List<UUID> findAlertIdsAwaitingPatientPastDeadline();

    /**
     * Confirma um alerta cujo prazo de resposta venceu sem resposta da paciente: ele
     * passa a {@code PENDING} e o médico é avisado.
     *
     * <p>A troca de status é condicional no banco, então esta chamada e a resposta da
     * paciente nunca vencem as duas. Só o vencedor publica o evento, então o médico
     * recebe no máximo um e-mail por alerta.
     *
     * @return {@code true} quando esta chamada fez a transição, {@code false} quando
     *         outro caminho chegou primeiro ou o alerta não existe mais
     */
    boolean confirmAlertWithoutPatientResponse(UUID alertId);
}
