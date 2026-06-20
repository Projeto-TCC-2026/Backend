package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReadingImportRequest(

        @NotNull(message = "ID do paciente é obrigatório")
        Long patientId,

        @NotNull(message = "ID do dispositivo é obrigatório")
        Long patientDeviceId,

        @NotBlank(message = "Arquivo de origem é obrigatório")
        @Size(max = 500, message = "Arquivo de origem deve ter no máximo 500 caracteres")
        String sourceFile
) {}
