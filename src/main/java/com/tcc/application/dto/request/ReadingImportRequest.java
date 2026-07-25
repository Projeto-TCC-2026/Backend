package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReadingImportRequest(

        @NotNull(message = "ID do paciente é obrigatório")
        UUID patientId,

        @NotNull(message = "ID do dispositivo é obrigatório")
        UUID patientDeviceId,

        @NotBlank(message = "Arquivo de origem é obrigatório")
        @Size(max = 500, message = "Arquivo de origem deve ter no máximo 500 caracteres")
        String sourceFile
) {}
