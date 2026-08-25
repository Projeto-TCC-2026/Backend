package com.tcc.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "field_type_presets")
public class FieldTypePreset {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 120)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FieldDataType dataType;

  private Double minValue;
  private Double maxValue;

  @Column(nullable = false, length = 40)
  private String inputStyle;

  @Column(nullable = false)
  private Boolean active = true;

  public FieldTypePreset() {
  }

  public FieldTypePreset(String name, String description, FieldDataType dataType,
      Double minValue, Double maxValue, String inputStyle) {
    this.name = name;
    this.description = description;
    this.dataType = dataType;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.inputStyle = inputStyle;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
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

  public FieldDataType getDataType() {
    return dataType;
  }

  public void setDataType(FieldDataType dataType) {
    this.dataType = dataType;
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

  public String getInputStyle() {
    return inputStyle;
  }

  public void setInputStyle(String inputStyle) {
    this.inputStyle = inputStyle;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }
}
