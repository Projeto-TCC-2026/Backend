package com.tcc.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "procedure_executions")
public class ProcedureExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_procedure_id", nullable = false)
    private PatientProcedure patientProcedure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_id", nullable = false)
    private Procedure procedure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false)
    private LocalDateTime executionDate;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @OneToMany(mappedBy = "procedureExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProcedurePhoto> procedurePhotos = new ArrayList<>();

    // Constructors
    public ProcedureExecution() {
    }

    public ProcedureExecution(PatientProcedure patientProcedure, Procedure procedure, Doctor doctor, Patient patient, LocalDateTime executionDate, String status) {
        this.patientProcedure = patientProcedure;
        this.procedure = procedure;
        this.doctor = doctor;
        this.patient = patient;
        this.executionDate = executionDate;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientProcedure getPatientProcedure() {
        return patientProcedure;
    }

    public void setPatientProcedure(PatientProcedure patientProcedure) {
        this.patientProcedure = patientProcedure;
    }

    public Procedure getProcedure() {
        return procedure;
    }

    public void setProcedure(Procedure procedure) {
        this.procedure = procedure;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDateTime executionDate) {
        this.executionDate = executionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public List<ProcedurePhoto> getProcedurePhotos() {
        return procedurePhotos;
    }

    public void setProcedurePhotos(List<ProcedurePhoto> procedurePhotos) {
        this.procedurePhotos = procedurePhotos;
    }
}
