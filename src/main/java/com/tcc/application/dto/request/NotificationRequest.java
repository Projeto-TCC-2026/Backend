package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationRequest(

        @NotNull(message = "ID do alerta é obrigatório")
        Long alertId,

        @NotNull(message = "ID do médico é obrigatório")
        Long doctorId,

        @NotBlank(message = "Mensagem é obrigatória")
        String message,

        @NotBlank(message = "Status é obrigatório")
        @Size(max = 50, message = "Status deve ter no máximo 50 caracteres")
        String status
) {}
