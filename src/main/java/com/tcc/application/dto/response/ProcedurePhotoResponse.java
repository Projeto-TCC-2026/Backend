package com.tcc.application.dto.response;

import java.time.LocalDateTime;

public record ProcedurePhotoResponse(
        Long id,
        Long procedureExecutionId,
        String imageUrl,
        String fileName,
        LocalDateTime uploadedAt
) {}
