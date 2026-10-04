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
 * <p>Ciclo de vida:
 * <pre>
 *   UNCONFIRMED --(2ª leitura seguida fora da faixa)--> PENDING --(médico)--> RESOLVED
 *        |
 *        +--(leitura dentro da faixa)--> NOT_CONFIRMED
 * </pre>
 *
 * <p>Só {@code PENDING} conta como alerta em aberto para o médico. É o único
 * status que dispara e-mail, e é o que os indicadores de "alertas pendentes" já
 * contavam antes destes dois status novos existirem — por isso nenhum número de
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

    private AlertStatus() {
    }
}
