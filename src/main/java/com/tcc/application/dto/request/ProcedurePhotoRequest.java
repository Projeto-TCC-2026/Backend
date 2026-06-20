package com.tcc.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcedurePhotoRequest(

        @NotNull(message = "ID da execução do procedimento é obrigatório")
        Long procedureExecutionId,

        @NotBlank(message = "URL da imagem é obrigatória")
        @Size(max = 500, message = "URL deve ter no máximo 500 caracteres")
        String imageUrl,

        @NotBlank(message = "Nome do arquivo é obrigatório")
        @Size(max = 255, message = "Nome do arquivo deve ter no máximo 255 caracteres")
        String fileName
) {}
