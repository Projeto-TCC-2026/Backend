package com.tcc.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "field_thresholds")
public class FieldThreshold {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "field_id", nullable = false)
  private DoctorProcedureField field;

  @Column(nullable = false)
  private Integer severityOrder;

  @Column(nullable = false, length = 80)
  private String label;

  @Column(length = 30)
  private String color;

  private Double minValue;
  private Double maxValue;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public DoctorProcedureField getField() {
    return field;
  }

  public void setField(DoctorProcedureField field) {
    this.field = field;
  }

  public Integer getSeverityOrder() {
    return severityOrder;
  }

  public void setSeverityOrder(Integer severityOrder) {
    this.severityOrder = severityOrder;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public Double getMinValue() {
    return minValue;
  }

  public void setMinValue(Double minValue) {
    this.minValue = minValue;
  }

  public Double getMaxValue() {
    return maxValue;
  }

  public void setMaxValue(Double maxValue) {
    this.maxValue = maxValue;
  }
}
