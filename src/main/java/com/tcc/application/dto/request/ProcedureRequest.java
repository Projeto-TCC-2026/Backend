package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProcedureRequest(

        @NotNull(message = "ID do médico é obrigatório")
        Long doctorId,

        @NotBlank(message = "Título é obrigatório")
        @Size(max = 255, message = "Título deve ter no máximo 255 caracteres")
        String title,

        String description,

        @Positive(message = "Duração estimada deve ser positiva")
        Integer estimatedDuration,

        Boolean active
) {}
