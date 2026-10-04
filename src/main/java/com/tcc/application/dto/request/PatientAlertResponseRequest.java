package com.tcc.application.dto.request;

import com.tcc.domain.model.PatientAlertAnswer;

import jakarta.validation.constraints.NotNull;

/**
 * Resposta da paciente à pergunta disparada por uma leitura grave.
 *
 * <p>{@code OK} significa que ela está bem: o alerta volta ao fluxo comum e o
 * médico não é avisado agora. {@code NOT_OK} significa que não está bem: o médico
 * é avisado na hora.
 *
 * <p>Valor fora desses dois é recusado com 400 pelo próprio Jackson, antes de
 * chegar ao service.
 */
public record PatientAlertResponseRequest(

        @NotNull(message = "Resposta é obrigatória e deve ser OK ou NOT_OK")
        PatientAlertAnswer answer
) {}
