package com.tcc.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Faixa normal de um tipo de leitura de sinal vital.
 *
 * Cada linha representa o intervalo considerado NORMAL para um readingType.
 * O alerta é gerado quando o valor medido sai dessa faixa. Limite nulo
 * significa "sem limite desse lado" — por exemplo, SPO2 tem apenas mínimo.
 */
@Entity
@Table(name = "reading_thresholds")
public class ReadingThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reading_type", nullable = false, length = 100)
    private String readingType;

    /** Limite inferior da faixa normal. Nulo significa sem limite inferior. */
    @Column(name = "normal_min")
    private Double normalMin;

    /** Limite superior da faixa normal. Nulo significa sem limite superior. */
    @Column(name = "normal_max")
    private Double normalMax;

    @Column(nullable = false, length = 50)
    private String severity;

    @Column(length = 255)
    private String label;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public ReadingThreshold() {
    }

    public ReadingThreshold(String readingType, Double normalMin, Double normalMax, String severity) {
        this.readingType = readingType;
        this.normalMin = normalMin;
        this.normalMax = normalMax;
        this.severity = severity;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReadingType() {
        return readingType;
    }

    public void setReadingType(String readingType) {
        this.readingType = readingType;
    }

    public Double getNormalMin() {
        return normalMin;
    }

    public void setNormalMin(Double normalMin) {
        this.normalMin = normalMin;
    }

    public Double getNormalMax() {
        return normalMax;
    }

    public void setNormalMax(Double normalMax) {
        this.normalMax = normalMax;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
