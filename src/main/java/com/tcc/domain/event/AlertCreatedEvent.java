package com.tcc.domain.event;

import com.tcc.domain.model.Alert;

/**
 * Publicado logo após o alerta ser persistido, ainda dentro da transação que o
 * criou. Os consumidores escutam em AFTER_COMMIT, então só reagem a alerta
 * efetivamente confirmado no banco.
 *
 * <p>O alerta viaja no evento, mas os relacionamentos LAZY dele não estarão
 * inicializados quando o consumidor rodar, porque a sessão que o carregou já foi
 * encerrada. Quem precisar de {@code patient.getUser()} deve recarregar a
 * entidade em transação própria.
 */
public record AlertCreatedEvent(Alert alert) {
}
