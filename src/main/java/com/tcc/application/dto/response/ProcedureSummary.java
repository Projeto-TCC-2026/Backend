package com.tcc.application.dto.response;

public record ProcedureSummary(
        Long id,
        String title,
        Integer estimatedDuration
) {}
