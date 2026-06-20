package com.tcc.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "procedures")
public class Procedure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

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

    public Procedure(Doctor doctor, String title) {
        this.doctor = doctor;
        this.title = title;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
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
