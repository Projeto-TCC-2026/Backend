package com.tcc.application.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record PatientRequest(

        @NotNull(message = "ID do usuário é obrigatório")
        Long userId,

        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String fullName,

        @NotBlank(message = "CPF é obrigatório")
        @Size(min = 11, max = 11, message = "CPF deve ter 11 caracteres")
        String cpf,

        @NotNull(message = "Data de nascimento é obrigatória")
        @Past(message = "Data de nascimento deve ser no passado")
        LocalDate birthDate,

        @Size(max = 20, message = "Gênero deve ter no máximo 20 caracteres")
        String gender,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String phone,

        Double weight,

        Double height
) {}
