package com.tcc.domain.model;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "checkins")
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_procedure_id", nullable = false)
    private PatientProcedure patientProcedure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckinSource source;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "manual_date")
    private LocalDate manualDate;

    @OneToMany(mappedBy = "checkin", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CheckinFieldValue> fieldValues = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PatientProcedure getPatientProcedure() {
        return patientProcedure;
    }

    public void setPatientProcedure(PatientProcedure patientProcedure) {
        this.patientProcedure = patientProcedure;
    }

    public CheckinSource getSource() {
        return source;
    }

    public void setSource(CheckinSource source) {
        this.source = source;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDate getManualDate() {
        return manualDate;
    }

    public void setManualDate(LocalDate manualDate) {
        this.manualDate = manualDate;
    }

    public List<CheckinFieldValue> getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(List<CheckinFieldValue> fieldValues) {
        this.fieldValues = fieldValues;
    }
}
