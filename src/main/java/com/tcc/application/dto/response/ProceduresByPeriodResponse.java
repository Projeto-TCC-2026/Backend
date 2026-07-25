package com.tcc.application.dto.response;

public record ProceduresByPeriodResponse(
        String period,
        Long totalProcedures
) {}
