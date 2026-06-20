package com.tcc.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patient_devices")
public class PatientDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false, length = 255)
    private String deviceIdentifier;

    @Column(nullable = false, length = 100)
    private String deviceType;

    @Column(length = 100)
    private String manufacturer;

    @Column(length = 100)
    private String model;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "patientDevice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReadingImport> readingImports = new ArrayList<>();

    @OneToMany(mappedBy = "patientDevice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HealthReading> healthReadings = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public PatientDevice() {
    }

    public PatientDevice(Patient patient, String deviceIdentifier, String deviceType) {
        this.patient = patient;
        this.deviceIdentifier = deviceIdentifier;
        this.deviceType = deviceType;
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

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
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

    public List<ReadingImport> getReadingImports() {
        return readingImports;
    }

    public void setReadingImports(List<ReadingImport> readingImports) {
        this.readingImports = readingImports;
    }

    public List<HealthReading> getHealthReadings() {
        return healthReadings;
    }

    public void setHealthReadings(List<HealthReading> healthReadings) {
        this.healthReadings = healthReadings;
    }
}
