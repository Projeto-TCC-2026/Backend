package com.tcc.application.dto.response;

import java.util.UUID;

import com.tcc.domain.model.FieldDataType;

public record FieldTypePresetResponse(
    UUID id,
    String name,
    String description,
    FieldDataType dataType,
    Double minValue,
    Double maxValue,
    String inputStyle) {
}
