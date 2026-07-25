package com.tcc.application.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HealthReadingRequest(

        @NotNull(message = "ID do paciente é obrigatório")
        UUID patientId,

        @NotNull(message = "ID do dispositivo é obrigatório")
        UUID patientDeviceId,

        UUID readingImportId,

        @NotBlank(message = "Tipo de leitura é obrigatório")
        @Size(max = 100, message = "Tipo de leitura deve ter no máximo 100 caracteres")
        String readingType,

        @NotBlank(message = "Valor é obrigatório")
        @Size(max = 100, message = "Valor deve ter no máximo 100 caracteres")
        String value,

        @Size(max = 50, message = "Unidade deve ter no máximo 50 caracteres")
        String unit,

        @NotNull(message = "Data/hora da medição é obrigatória")
        LocalDateTime measuredAt
) {}
