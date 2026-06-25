package com.tcc.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "health_readings")
public class HealthReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_device_id", nullable = false)
    private PatientDevice patientDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_import_id")
    private ReadingImport readingImport;

    @Column(nullable = false, length = 100)
    private String readingType;

    @Column(name = "reading_value", nullable = false, length = 100)
    private String value;

    @Column(length = 50)
    private String unit;

    @Column(nullable = false)
    private LocalDateTime measuredAt;

    @OneToMany(mappedBy = "healthReading", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Alert> alerts = new ArrayList<>();

    // Constructors
    public HealthReading() {
    }

    public HealthReading(Patient patient, PatientDevice patientDevice, String readingType, String value, LocalDateTime measuredAt) {
        this.patient = patient;
        this.patientDevice = patientDevice;
        this.readingType = readingType;
        this.value = value;
        this.measuredAt = measuredAt;
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

    public ReadingImport getReadingImport() {
        return readingImport;
    }

    public void setReadingImport(ReadingImport readingImport) {
        this.readingImport = readingImport;
    }

    public String getReadingType() {
        return readingType;
    }

    public void setReadingType(String readingType) {
        this.readingType = readingType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDateTime getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(LocalDateTime measuredAt) {
        this.measuredAt = measuredAt;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }
}
