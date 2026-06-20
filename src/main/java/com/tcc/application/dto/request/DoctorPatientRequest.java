package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record DoctorPatientRequest(

        @NotNull(message = "ID do médico é obrigatório")
        Long doctorId,

        @NotNull(message = "ID do paciente é obrigatório")
        Long patientId
) {}
