package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        String code,
        String token,

        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
        String password,

        @NotBlank(message = "A confirmação da senha é obrigatória")
        String passwordConfirmation
) {
    public ResetPasswordRequest(String token, String password, String passwordConfirmation) {
        this(token, token, password, passwordConfirmation);
    }

    public String codeOrToken() {
        if (code != null && !code.isBlank()) {
            return code.trim();
        }
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        return "";
    }
}
