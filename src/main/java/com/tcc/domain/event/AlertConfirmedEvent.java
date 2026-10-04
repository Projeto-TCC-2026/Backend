package com.tcc.domain.event;

import com.tcc.domain.model.Alert;
import com.tcc.domain.model.HealthReading;

/**
 * Publicado quando o alerta passa a PENDING, ou seja, quando o médico precisa ser
 * avisado. É este o evento que dispara o e-mail — {@link AlertCreatedEvent} avisa
 * apenas o paciente.
 *
 * <p>Três caminhos chegam aqui, distinguidos por {@code reason}:
 * <ul>
 *   <li>{@link AlertConfirmationReason#DUAS_LEITURAS} — fluxo comum. Carrega as
 *       duas leituras: a que criou o alerta ({@code firstReading}) e a que o
 *       confirmou ({@code confirmingReading}). O e-mail mostra ambas, para o
 *       médico ver a progressão e não só o último valor.</li>
 *   <li>{@link AlertConfirmationReason#PACIENTE_NAO_ESTA_BEM} — leitura grave e a
 *       paciente respondeu que não está bem.</li>
 *   <li>{@link AlertConfirmationReason#SEM_RESPOSTA} — leitura grave e a paciente
 *       não respondeu no prazo.</li>
 * </ul>
 *
 * <p>Nos dois caminhos de leitura grave existe uma única leitura, a grave, que vai
 * em {@code firstReading}; {@code confirmingReading} fica nulo, porque nenhuma
 * segunda medição participou da decisão. O e-mail mostra só a leitura grave.
 *
 * <p>Os consumidores escutam em AFTER_COMMIT, então só reagem a confirmação
 * efetivamente gravada. Os relacionamentos LAZY das entidades não estarão
 * inicializados quando o consumidor rodar: quem precisar de
 * {@code patient.getUser()} deve recarregar em transação própria. Os campos
 * escalares das leituras (valor, unidade, horário) são lidos aqui dentro do mesmo
 * commit, então viajam preenchidos.
 */
public record AlertConfirmedEvent(
        Alert alert,
        HealthReading firstReading,
        HealthReading confirmingReading,
        AlertConfirmationReason reason
) {

    /** Fluxo comum: duas leituras seguidas fora da faixa normal. */
    public static AlertConfirmedEvent byTwoReadings(Alert alert, HealthReading firstReading,
                                                    HealthReading confirmingReading) {
        return new AlertConfirmedEvent(alert, firstReading, confirmingReading,
                AlertConfirmationReason.DUAS_LEITURAS);
    }

    /**
     * Leitura grave: só a leitura grave participou da decisão, então
     * {@code confirmingReading} fica nulo.
     */
    public static AlertConfirmedEvent bySevereReading(Alert alert, HealthReading severeReading,
                                                      AlertConfirmationReason reason) {
        return new AlertConfirmedEvent(alert, severeReading, null, reason);
    }
}
