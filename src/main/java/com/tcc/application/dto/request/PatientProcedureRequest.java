package com.tcc.application.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * O paciente vem pela URL e o médico é derivado do token do usuário autenticado,
 * por isso nenhum dos dois aparece aqui.
 */
public record PatientProcedureRequest(

        @NotNull(message = "ID do procedimento é obrigatório")
        UUID procedureId,

        @NotNull(message = "Data de início é obrigatória")
        LocalDate startDate,

        LocalDate endDate,

        @NotBlank(message = "Status é obrigatório")
        @Size(max = 50, message = "Status deve ter no máximo 50 caracteres")
        String status,

        String notes
) {}
