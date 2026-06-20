package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PatientDeviceRequest(

        @NotNull(message = "ID do paciente é obrigatório")
        Long patientId,

        @NotBlank(message = "Identificador do dispositivo é obrigatório")
        @Size(max = 255, message = "Identificador deve ter no máximo 255 caracteres")
        String deviceIdentifier,

        @NotBlank(message = "Tipo do dispositivo é obrigatório")
        @Size(max = 100, message = "Tipo do dispositivo deve ter no máximo 100 caracteres")
        String deviceType,

        @Size(max = 100, message = "Fabricante deve ter no máximo 100 caracteres")
        String manufacturer,

        @Size(max = 100, message = "Modelo deve ter no máximo 100 caracteres")
        String model,

        Boolean active
) {}
