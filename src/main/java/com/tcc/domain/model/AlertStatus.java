package com.tcc.domain.model;

/**
 * Valores aceitos em {@code alerts.status}.
 *
 * <p>Deliberadamente constantes de String, e não um enum: a coluna é
 * {@code VARCHAR(50)} e o JPQL do dashboard compara com literal
 * ({@code AlertRepository.countPendingAlertsByHospitalId}), então o projeto já
 * trata status como texto. Trocar por enum mudaria o mapeamento de uma coluna em
 * uso e está fora do escopo desta task.
 *
 * <p>Ciclo de vida do fluxo comum:
 * <pre>
 *   UNCONFIRMED --(2ª leitura seguida fora da faixa)--> PENDING --(médico)--> RESOLVED
 *        |
 *        +--(leitura dentro da faixa)--> NOT_CONFIRMED
 * </pre>
 *
 * <p>Ciclo de vida do valor GRAVE, que substitui o fluxo comum quando a leitura
 * está dentro da faixa grave do tipo:
 * <pre>
 *   AWAITING_PATIENT --(paciente: "não estou bem")--> PENDING --(médico)--> RESOLVED
 *        |
 *        +--(prazo de 10 min vencido sem resposta)--> PENDING
 *        |
 *        +--(paciente: "estou bem")--> UNCONFIRMED (volta ao fluxo comum)
 * </pre>
 *
 * <p>Só {@code PENDING} conta como alerta em aberto para o médico. É o único
 * status que dispara e-mail, e é o que os indicadores de "alertas pendentes" já
 * contavam antes destes status novos existirem — por isso nenhum número de
 * dashboard muda de significado.
 */
public final class AlertStatus {

    /**
     * Primeira leitura fora da faixa normal. Ainda não é um problema confirmado:
     * gera push ao paciente, mas nenhum e-mail ao médico.
     */
    public static final String UNCONFIRMED = "UNCONFIRMED";

    /**
     * Confirmado por duas leituras seguidas fora da faixa. Este é o status que o
     * JPQL de contagem do dashboard filtra, então qualquer outra grafia tornaria o
     * alerta invisível nos indicadores de "alertas pendentes".
     */
    public static final String PENDING = "PENDING";

    /**
     * A leitura seguinte voltou ao normal, então o desvio não se confirmou. Estado
     * final: não vira PENDING depois.
     */
    public static final String NOT_CONFIRMED = "NOT_CONFIRMED";

    /** Tratado pelo médico. Estado final. */
    public static final String RESOLVED = "RESOLVED";

    /**
     * Leitura em faixa GRAVE: o alerta espera a paciente responder se está bem.
     * Gera push com a pergunta e nenhum e-mail ao médico. Sai deste status pela
     * resposta da paciente ou pelo vencimento do prazo de 10 minutos.
     *
     * <p>Não conta como alerta em aberto para o médico: só {@code PENDING} conta,
     * e é para lá que este status vai quando o médico precisa ser avisado.
     */
    public static final String AWAITING_PATIENT = "AWAITING_PATIENT";

    private AlertStatus() {
    }
}
