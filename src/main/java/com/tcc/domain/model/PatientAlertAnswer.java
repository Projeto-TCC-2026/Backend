package com.tcc.domain.model;

/**
 * Resposta da paciente à pergunta disparada por uma leitura GRAVE.
 *
 * <p>Gravada em {@code alerts.patient_response}, que é {@code VARCHAR(20)}. É um
 * enum de verdade, e não constantes de String como {@link AlertStatus}: a coluna
 * nasceu na V36 e nenhum JPQL compara com literal dela, então não há mapeamento
 * em uso para preservar.
 */
public enum PatientAlertAnswer {

    /** A paciente respondeu que está bem. O alerta volta ao fluxo comum. */
    OK,

    /** A paciente respondeu que não está bem. O médico é avisado na hora. */
    NOT_OK
}
