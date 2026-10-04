package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.tcc.domain.model.PatientAlertAnswer;

/**
 * Resultado do registro da resposta da paciente a um alerta de leitura grave.
 *
 * <p>{@code alertStatus} é o status para o qual o alerta foi levado:
 * {@code PENDING} quando ela respondeu que não está bem, e {@code UNCONFIRMED}
 * quando respondeu que está bem.
 *
 * <p>{@code doctorNotified} diz se o aviso ao médico foi disparado por esta
 * resposta. Verdadeiro apenas no caso {@code NOT_OK}.
 *
 * <p>DTO próprio, e não o {@code AlertResponse} usado na listagem: o contrato de
 * {@code GET /api/mobile/alerts} não muda por causa deste endpoint novo.
 */
public record PatientAlertAnswerResponse(
        UUID alertId,
        PatientAlertAnswer answer,
        LocalDateTime respondedAt,
        String alertStatus,
        boolean doctorNotified
) {}
