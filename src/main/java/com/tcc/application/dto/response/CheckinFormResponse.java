package com.tcc.application.dto.response;

import java.util.List;
import java.util.UUID;

import com.tcc.domain.model.FieldDataType;

public record CheckinFormResponse(
    UUID patientProcedureId,
    ProcedureSummary procedure,
    List<Field> fields) {
  public record Field(
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
      Boolean normalBoolean) {
  }
}
