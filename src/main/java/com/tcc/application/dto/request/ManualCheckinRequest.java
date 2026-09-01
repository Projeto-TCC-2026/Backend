package com.tcc.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ManualCheckinRequest(
        @NotNull(message = "Respostas do check-in são obrigatórias")
        List<@Valid FieldValue> fields) {

    public record FieldValue(
            @NotNull(message = "ID do campo é obrigatório")
            UUID fieldId,
            @Size(max = 10000, message = "Valor deve ter no máximo 10000 caracteres")
            String value,
            @Size(max = 2048, message = "URL da foto deve ter no máximo 2048 caracteres")
            String photoUrl) {
    }
}
