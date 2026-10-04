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

    /**
     * Limite inferior do que é fisiologicamente possível medir. Valor abaixo disso
     * é defeito de sensor, não quadro clínico. Nulo desliga o filtro deste lado.
     */
    @Column(name = "plausible_min")
    private Double plausibleMin;

    /**
     * Limite superior do que é fisiologicamente possível medir. Nulo desliga o
     * filtro deste lado.
     */
    @Column(name = "plausible_max")
    private Double plausibleMax;

    /**
     * Limite inferior do que é GRAVE. Valor igual ou abaixo dele é grave o
     * bastante para perguntar à paciente na hora, em vez de esperar a segunda
     * leitura do fluxo comum. Nulo desliga o lado inferior.
     */
    @Column(name = "severe_min")
    private Double severeMin;

    /**
     * Limite superior do que é GRAVE. Valor igual ou acima dele é grave. Nulo
     * desliga o lado superior — é o caso de SPO2 e TEMPERATURE, em que o valor
     * alto não justifica interromper a paciente.
     */
    @Column(name = "severe_max")
    private Double severeMax;

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

    public Double getPlausibleMin() {
        return plausibleMin;
    }

    public void setPlausibleMin(Double plausibleMin) {
        this.plausibleMin = plausibleMin;
    }

    public Double getPlausibleMax() {
        return plausibleMax;
    }

    public void setPlausibleMax(Double plausibleMax) {
        this.plausibleMax = plausibleMax;
    }

    /**
     * Verdadeiro quando o valor está fora do que é possível medir. Sem faixa
     * plausível cadastrada nada é implausível: o filtro fica desligado para o tipo.
     *
     * <p>Os limites são inclusivos no plausível, como já acontece na faixa normal:
     * valor igual ao limite é aceito.
     */
    public boolean isImplausible(Double value) {
        if (value == null) {
            return false;
        }
        if (plausibleMin != null && value < plausibleMin) {
            return true;
        }
        return plausibleMax != null && value > plausibleMax;
    }

    public Double getSevereMin() {
        return severeMin;
    }

    public void setSevereMin(Double severeMin) {
        this.severeMin = severeMin;
    }

    public Double getSevereMax() {
        return severeMax;
    }

    public void setSevereMax(Double severeMax) {
        this.severeMax = severeMax;
    }

    /**
     * Verdadeiro quando o valor está na faixa GRAVE. Sem faixa grave cadastrada
     * nada é grave: o tipo segue apenas o fluxo comum de confirmação por duas
     * leituras.
     *
     * <p>Atenção à direção dos limites, que é o oposto das outras duas faixas: na
     * normal e na plausível o limite pertence ao que é aceitável, aqui o limite
     * pertence ao que é grave. A comparação usa {@code <=} e {@code >=}, então
     * {@code severeMin = 40} torna 40 grave e deixa 41 fora, e
     * {@code severeMax = 131} torna 131 grave e deixa 130 fora.
     */
    public boolean isSevere(Double value) {
        if (value == null) {
            return false;
        }
        if (severeMin != null && value <= severeMin) {
            return true;
        }
        return severeMax != null && value >= severeMax;
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
