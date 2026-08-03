package com.tcc.application.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record DoctorProcedureRequest(

        @NotNull(message = "ID do médico é obrigatório")
        UUID doctorId
) {}
