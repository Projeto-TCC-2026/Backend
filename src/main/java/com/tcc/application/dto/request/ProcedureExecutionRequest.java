package com.tcc.application.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcedureExecutionRequest(

        @NotNull(message = "ID do procedimento do paciente é obrigatório")
        Long patientProcedureId,

        @NotNull(message = "ID do procedimento é obrigatório")
        Long procedureId,

        @NotNull(message = "ID do médico é obrigatório")
        Long doctorId,

        @NotNull(message = "ID do paciente é obrigatório")
        Long patientId,

        @NotNull(message = "Data de execução é obrigatória")
        LocalDateTime executionDate,

        @NotBlank(message = "Status é obrigatório")
        @Size(max = 50, message = "Status deve ter no máximo 50 caracteres")
        String status,

        String observations
) {}
