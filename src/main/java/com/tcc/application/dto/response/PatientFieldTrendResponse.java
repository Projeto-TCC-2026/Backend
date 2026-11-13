package com.tcc.application.dto.response;

import com.tcc.domain.model.FieldDataType;
import java.util.List;
import java.util.UUID;

public record PatientFieldTrendResponse(
        UUID fieldId,
        String name,
        FieldDataType dataType,
        String unit,
        List<DailyAverageResponse> dailyAverages) {
}
