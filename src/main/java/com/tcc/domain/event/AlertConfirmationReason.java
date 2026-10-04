package com.tcc.domain.event;

/**
 * Por que o alerta foi confirmado e o médico está sendo avisado.
 *
 * <p>Viaja em {@link AlertConfirmedEvent} e define o texto do e-mail: cada motivo
 * descreve uma evidência diferente, e o médico precisa saber qual delas chegou
 * até ele.
 *
 * <p>Os nomes estão em português porque são termos do domínio do produto, não
 * identificadores técnicos — é o mesmo critério que mantém as mensagens de erro
 * e as descrições de API em português. O enum não é persistido em nenhuma coluna.
 */
public enum AlertConfirmationReason {

    /** Fluxo comum: duas leituras seguidas fora da faixa normal, em até 2h. */
    DUAS_LEITURAS,

    /** Leitura grave e a paciente respondeu que não está bem. */
    PACIENTE_NAO_ESTA_BEM,

    /** Leitura grave e a paciente não respondeu dentro do prazo de 10 minutos. */
    SEM_RESPOSTA
}
