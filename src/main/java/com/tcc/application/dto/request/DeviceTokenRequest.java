package com.tcc.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Registro de token de push do aplicativo mobile")
public record DeviceTokenRequest(

        @Schema(description = "Token de push emitido pelo serviço de notificação do dispositivo")
        @NotBlank(message = "Token do dispositivo é obrigatório")
        @Size(max = 512, message = "Token deve ter no máximo 512 caracteres")
        String token,

        @Schema(description = "Plataforma do dispositivo", example = "ANDROID")
        @NotBlank(message = "Plataforma é obrigatória")
        @Size(max = 20, message = "Plataforma deve ter no máximo 20 caracteres")
        String platform,

        @Schema(description = "Identificador do aparelho, opcional")
        @Size(max = 255, message = "Identificador do dispositivo deve ter no máximo 255 caracteres")
        String deviceId
) {}
