package com.tcc.application.dto.response;

import java.util.List;
import java.util.UUID;

import com.tcc.domain.model.FieldDataType;

public record DoctorProcedureFieldResponse(
    UUID id,
    String name,
    String description,
    String unit,
    FieldDataType dataType,
    String metricKey,
    Boolean required,
    Integer displayOrder,
    Double minValue,
    Double maxValue,
    Boolean normalBoolean,
    Boolean active,
    List<FieldThresholdResponse> thresholds) {
  public record FieldThresholdResponse(
      UUID id,
      Integer severityOrder,
      String label,
      String color,
      Double minValue,
      Double maxValue) {
  }
}
