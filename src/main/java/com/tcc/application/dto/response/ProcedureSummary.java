package com.tcc.application.dto.response;

import java.util.UUID;

public record ProcedureSummary(
        UUID id,
        String title,
        Integer estimatedDuration
) {}
