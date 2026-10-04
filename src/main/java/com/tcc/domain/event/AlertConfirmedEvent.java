package com.tcc.domain.event;

import com.tcc.domain.model.Alert;
import com.tcc.domain.model.HealthReading;

/**
 * Publicado quando uma segunda leitura seguida confirma o desvio e o alerta passa
 * de UNCONFIRMED para PENDING. É este o evento que dispara o e-mail ao médico —
 * {@link AlertCreatedEvent} avisa apenas o paciente.
 *
 * <p>Como o alerta só é confirmado por duas leituras, o evento carrega as duas: a
 * que criou o alerta ({@code firstReading}) e a que o confirmou
 * ({@code confirmingReading}). O e-mail mostra ambas, para o médico ver a
 * progressão e não só o último valor.
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
        HealthReading confirmingReading
) {
}
