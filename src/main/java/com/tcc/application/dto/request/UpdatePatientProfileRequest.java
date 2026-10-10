package com.tcc.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados editáveis pelo próprio paciente autenticado.
 *
 * <p>CPF, dados clínicos (tipo sanguíneo, peso, altura) e dados de endereço
 * só podem ser alterados por um médico via {@code PUT /api/patients/{id}}.
 */
public record UpdatePatientProfileRequest(

        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String fullName,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String phone,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail deve ser válido")
        @Size(max = 255, message = "E-mail deve ter no máximo 255 caracteres")
        String email
) {}
