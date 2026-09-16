package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Dados editáveis pelo próprio paciente autenticado.
 *
 * <p>CPF, e-mail e tipo sanguíneo são intencionalmente excluídos: campos de
 * identificação e dados clínicos sensíveis só podem ser alterados por um médico
 * via {@code PUT /api/patients/{id}}.
 */
public record UpdatePatientProfileRequest(

        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String fullName,

        @Past(message = "Data de nascimento deve ser no passado")
        LocalDate birthDate,

        @Size(max = 20, message = "Gênero deve ter no máximo 20 caracteres")
        String gender,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String phone,

        @Size(max = 500, message = "Endereço deve ter no máximo 500 caracteres")
        String address,

        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
        String city,

        @Size(min = 2, max = 2, message = "Estado deve ter 2 caracteres (sigla)")
        @Pattern(regexp = "[A-Z]{2}", message = "Estado deve ser uma sigla válida (ex: SP, RJ)")
        String state,

        @Size(max = 10, message = "CEP deve ter no máximo 10 caracteres")
        String zipCode,

        Double weight,

        Double height
) {}
