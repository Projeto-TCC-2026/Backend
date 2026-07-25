package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DoctorPatientRequest(

        @NotNull(message = "ID do médico é obrigatório")
        UUID doctorId,

        @NotNull(message = "ID do paciente é obrigatório")
        UUID patientId
) {}
