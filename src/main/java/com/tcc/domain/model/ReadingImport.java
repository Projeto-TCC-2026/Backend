package com.tcc.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reading_imports")
public class ReadingImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_device_id", nullable = false)
    private PatientDevice patientDevice;

    @Column(nullable = false, length = 500)
    private String sourceFile;

    @Column(nullable = false)
    private LocalDateTime importedAt;

    @OneToMany(mappedBy = "readingImport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HealthReading> healthReadings = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        importedAt = LocalDateTime.now();
    }

    // Constructors
    public ReadingImport() {
    }

    public ReadingImport(Patient patient, PatientDevice patientDevice, String sourceFile) {
        this.patient = patient;
        this.patientDevice = patientDevice;
        this.sourceFile = sourceFile;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PatientDevice getPatientDevice() {
        return patientDevice;
    }

    public void setPatientDevice(PatientDevice patientDevice) {
        this.patientDevice = patientDevice;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }

    public List<HealthReading> getHealthReadings() {
        return healthReadings;
    }

    public void setHealthReadings(List<HealthReading> healthReadings) {
        this.healthReadings = healthReadings;
    }
}
