package com.tcc.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AggregatedManualCheckinRequest(
        @NotEmpty(message = "Procedimentos do check-in são obrigatórios")
        List<@NotNull UUID> patientProcedureIds,
        @NotBlank(message = "A chave de idempotência é obrigatória")
        @Size(max = 128, message = "A chave de idempotência deve ter no máximo 128 caracteres")
        String idempotencyKey,
        @Size(max = 128, message = "A versão da configuração deve ter no máximo 128 caracteres")
        String configurationVersion,
        @NotNull(message = "Respostas do check-in são obrigatórias")
        @Valid List<ManualCheckinRequest.FieldValue> fields) {
}
