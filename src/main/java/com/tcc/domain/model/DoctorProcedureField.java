package com.tcc.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "doctor_procedure_fields")
public class DoctorProcedureField {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "doctor_procedure_id", nullable = false)
  private DoctorProcedure doctorProcedure;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(length = 40)
  private String unit;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FieldDataType dataType;

  @Column(length = 60)
  private String metricKey;

  @Column(nullable = false)
  private Boolean required = false;

  @Column(nullable = false)
  private Integer displayOrder = 0;

  private Double minValue;
  private Double maxValue;

  private Boolean normalBoolean;

  @Column(nullable = false)
  private Boolean active = true;

  @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<FieldThreshold> thresholds = new ArrayList<>();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public DoctorProcedure getDoctorProcedure() {
    return doctorProcedure;
  }

  public void setDoctorProcedure(DoctorProcedure doctorProcedure) {
    this.doctorProcedure = doctorProcedure;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public FieldDataType getDataType() {
    return dataType;
  }

  public void setDataType(FieldDataType dataType) {
    this.dataType = dataType;
  }

  public String getMetricKey() {
    return metricKey;
  }

  public void setMetricKey(String metricKey) {
    this.metricKey = metricKey;
  }

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public Integer getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(Integer displayOrder) {
    this.displayOrder = displayOrder;
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

  public Boolean getNormalBoolean() {
    return normalBoolean;
  }

  public void setNormalBoolean(Boolean normalBoolean) {
    this.normalBoolean = normalBoolean;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public List<FieldThreshold> getThresholds() {
    return thresholds;
  }

  public void setThresholds(List<FieldThreshold> thresholds) {
    this.thresholds = thresholds;
  }
}
