package com.tcc.application.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Leitura de sinal vital submetida para avaliação de risco.
 *
 * A leitura não é persistida nesta versão: o backend apenas decide se o valor
 * está fora da faixa normal cadastrada em reading_thresholds.
 */
public record AlertEvaluationRequest(

        @NotNull(message = "ID do paciente é obrigatório")
        UUID patientId,

        @NotBlank(message = "Tipo de leitura é obrigatório")
        @Size(max = 100, message = "Tipo de leitura deve ter no máximo 100 caracteres")
        String readingType,

        @NotNull(message = "Valor da leitura é obrigatório")
        Double value,

        @NotNull(message = "Data/hora da medição é obrigatória")
        LocalDateTime measuredAt
) {}
