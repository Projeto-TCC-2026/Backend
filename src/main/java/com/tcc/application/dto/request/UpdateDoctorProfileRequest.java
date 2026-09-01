package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDoctorProfileRequest(

        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String fullName,

        @Size(max = 100, message = "Especialidade deve ter no máximo 100 caracteres")
        String specialty,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String phone
) {
}
