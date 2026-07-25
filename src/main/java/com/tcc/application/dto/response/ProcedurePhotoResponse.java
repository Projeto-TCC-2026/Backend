package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProcedurePhotoResponse(
        UUID id,
        UUID procedureExecutionId,
        String imageUrl,
        String fileName,
        LocalDateTime uploadedAt
) {}
