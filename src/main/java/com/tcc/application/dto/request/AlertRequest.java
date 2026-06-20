package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlertRequest(

        @NotNull(message = "ID do paciente é obrigatório")
        Long patientId,

        Long healthReadingId,

        @NotBlank(message = "Severidade é obrigatória")
        @Size(max = 50, message = "Severidade deve ter no máximo 50 caracteres")
        String severity,

        @NotBlank(message = "Título é obrigatório")
        @Size(max = 255, message = "Título deve ter no máximo 255 caracteres")
        String title,

        String description,

        @NotBlank(message = "Status é obrigatório")
        @Size(max = 50, message = "Status deve ter no máximo 50 caracteres")
        String status
) {}
