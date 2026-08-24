package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo enviado pela tela "Seja bem-vindo" para definir a primeira senha da conta. */
public record AccountActivationRequest(
        @NotBlank(message = "O token é obrigatório")
        String token,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
        String password,

        @NotBlank(message = "A confirmação da senha é obrigatória")
        String passwordConfirmation
) {
}
