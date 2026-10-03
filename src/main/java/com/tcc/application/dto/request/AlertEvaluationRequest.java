package com.tcc.application.dto.request;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Leitura de sinal vital submetida para avaliação de risco.
 *
 * <p>Toda leitura recebida é gravada em {@code health_readings}, dentro ou fora da
 * faixa normal, antes da avaliação. O trio
 * {@code patientId + readingType + measuredAt} é a chave de idempotência: leitura
 * repetida não é gravada de novo e não gera alerta.
 *
 * <p><strong>Convenção de fuso:</strong> {@code measuredAt} chega sempre com fuso
 * explícito e é convertido para UTC antes de qualquer uso — tanto para gravar quanto
 * para comparar com leitura já recebida. Por isso
 * {@code 2026-07-01T14:32:00Z} e {@code 2026-07-01T11:32:00-03:00} são o mesmo
 * instante e, portanto, a mesma leitura.
 *
 * <p>Horário sem fuso, como {@code 2026-07-01T14:32:00}, é recusado com 400: sem o
 * fuso não há como saber a que instante a medição se refere, e adivinhar produziria
 * leitura duplicada ou horário errado no aviso ao médico.
 *
 * <p>{@code measuredAt} é o horário da medição, não o da chegada: a leitura pode
 * chegar atrasada pela fila, e é o horário da medição que o médico precisa ver.
 */
public record AlertEvaluationRequest(

        @NotNull(message = "ID do paciente é obrigatório")
        UUID patientId,

        @NotBlank(message = "Tipo de leitura é obrigatório")
        @Size(max = 100, message = "Tipo de leitura deve ter no máximo 100 caracteres")
        String readingType,

        @NotNull(message = "Valor da leitura é obrigatório")
        Double value,

        @NotNull(message = "Data/hora da medição é obrigatória, com fuso explícito")
        OffsetDateTime measuredAt,

        @Size(max = 50, message = "Unidade deve ter no máximo 50 caracteres")
        String unit
) {}
