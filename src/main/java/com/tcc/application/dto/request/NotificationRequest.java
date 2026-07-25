package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record NotificationRequest(

        @NotNull(message = "ID do alerta é obrigatório")
        UUID alertId,

        @NotNull(message = "ID do médico é obrigatório")
        UUID doctorId,

        @NotBlank(message = "Mensagem é obrigatória")
        String message,

        @NotBlank(message = "Status é obrigatório")
        @Size(max = 50, message = "Status deve ter no máximo 50 caracteres")
        String status
) {}
