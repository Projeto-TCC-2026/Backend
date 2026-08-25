package com.tcc.application.dto.request;

import java.util.List;

import com.tcc.domain.model.FieldDataType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DoctorProcedureFieldRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 2000) String description,
    @Size(max = 40) String unit,
    @NotNull FieldDataType dataType,
    @Size(max = 60) String metricKey,
    Boolean required,
    Integer displayOrder,
    Double minValue,
    Double maxValue,
    Boolean normalBoolean,
    List<FieldThresholdRequest> thresholds) {
  public record FieldThresholdRequest(
      @NotNull Integer severityOrder,
      @NotBlank @Size(max = 80) String label,
      @Size(max = 30) String color,
      Double minValue,
      Double maxValue) {
  }
}
