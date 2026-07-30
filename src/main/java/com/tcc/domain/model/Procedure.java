package com.tcc.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "procedures")
public class Procedure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Integer estimatedDuration;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "procedure", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DoctorProcedure> doctorProcedures = new ArrayList<>();

    @OneToMany(mappedBy = "procedure", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PatientProcedure> patientProcedures = new ArrayList<>();

    @OneToMany(mappedBy = "procedure", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProcedureExecution> procedureExecutions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public Procedure() {
    }

    public Procedure(Hospital hospital, String title) {
        this.hospital = hospital;
        this.title = title;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(Integer estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<DoctorProcedure> getDoctorProcedures() {
        return doctorProcedures;
    }

    public void setDoctorProcedures(List<DoctorProcedure> doctorProcedures) {
        this.doctorProcedures = doctorProcedures;
    }

    public List<PatientProcedure> getPatientProcedures() {
        return patientProcedures;
    }

    public void setPatientProcedures(List<PatientProcedure> patientProcedures) {
        this.patientProcedures = patientProcedures;
    }

    public List<ProcedureExecution> getProcedureExecutions() {
        return procedureExecutions;
    }

    public void setProcedureExecutions(List<ProcedureExecution> procedureExecutions) {
        this.procedureExecutions = procedureExecutions;
    }
}
