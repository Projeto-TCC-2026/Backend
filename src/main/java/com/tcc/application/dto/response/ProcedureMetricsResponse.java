package com.tcc.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProcedureMetricsResponse(
        UUID procedureId,
        LocalDate requestedStartDate,
        LocalDate requestedEndDate,
        LocalDate dataStartDate,
        LocalDate dataEndDate,
        List<ProcedureFieldMetricsResponse> fields) {
}
