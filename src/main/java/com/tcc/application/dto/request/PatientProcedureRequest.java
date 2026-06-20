package com.tcc.application.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PatientProcedureRequest(

        @NotNull(message = "ID do paciente é obrigatório")
        Long patientId,

        @NotNull(message = "ID do procedimento é obrigatório")
        Long procedureId,

        @NotNull(message = "ID do médico é obrigatório")
        Long doctorId,

        @NotNull(message = "Data de início é obrigatória")
        LocalDate startDate,

        LocalDate endDate,

        @NotBlank(message = "Status é obrigatório")
        @Size(max = 50, message = "Status deve ter no máximo 50 caracteres")
        String status,

        String notes
) {}
