package com.tcc.application.dto.response;

import com.tcc.domain.model.FieldDataType;
import java.util.List;
import java.util.UUID;

public record ProcedureFieldMetricsResponse(
        UUID fieldId,
        List<UUID> sourceFieldIds,
        String name,
        FieldDataType dataType,
        String unit,
        List<DailyAverageResponse> dailyAverages) {
}
