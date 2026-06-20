package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DoctorRequest(

        @NotNull(message = "ID do usuário é obrigatório")
        Long userId,

        @NotNull(message = "ID do hospital é obrigatório")
        Long hospitalId,

        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String fullName,

        @NotBlank(message = "CPF é obrigatório")
        @Size(min = 11, max = 11, message = "CPF deve ter 11 caracteres")
        String cpf,

        @NotBlank(message = "CRM é obrigatório")
        @Size(max = 20, message = "CRM deve ter no máximo 20 caracteres")
        String crm,

        @Size(max = 100, message = "Especialidade deve ter no máximo 100 caracteres")
        String specialty,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String phone
) {}
