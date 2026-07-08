package com.tcc.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(length = 20)
    private String gender;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 2)
    private String state;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "blood_type", length = 10)
    private String bloodType;

    @Column
    private Double weight;

    @Column
    private Double height;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DoctorPatient> doctorPatients = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PatientDevice> patientDevices = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PatientProcedure> patientProcedures = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HealthReading> healthReadings = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Alert> alerts = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProcedureExecution> procedureExecutions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Patient() {
    }

    public Patient(User user, String fullName, String cpf, LocalDate birthDate) {
        this.user = user;
        this.fullName = fullName;
        this.cpf = cpf;
        this.birthDate = birthDate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<DoctorPatient> getDoctorPatients() {
        return doctorPatients;
    }

    public void setDoctorPatients(List<DoctorPatient> doctorPatients) {
        this.doctorPatients = doctorPatients;
    }

    public List<PatientDevice> getPatientDevices() {
        return patientDevices;
    }

    public void setPatientDevices(List<PatientDevice> patientDevices) {
        this.patientDevices = patientDevices;
    }

    public List<PatientProcedure> getPatientProcedures() {
        return patientProcedures;
    }

    public void setPatientProcedures(List<PatientProcedure> patientProcedures) {
        this.patientProcedures = patientProcedures;
    }

    public List<HealthReading> getHealthReadings() {
        return healthReadings;
    }

    public void setHealthReadings(List<HealthReading> healthReadings) {
        this.healthReadings = healthReadings;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }

    public List<ProcedureExecution> getProcedureExecutions() {
        return procedureExecutions;
    }

    public void setProcedureExecutions(List<ProcedureExecution> procedureExecutions) {
        this.procedureExecutions = procedureExecutions;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // Business methods
    public void inactivate() {
        this.active = false;
    }

    public boolean isActive() {
        return active != null && active;
    }

    // Métodos de negócio para Procedimentos Realizados
    public void addProcedureExecution(ProcedureExecution procedureExecution) {
        this.procedureExecutions.add(procedureExecution);
        procedureExecution.setPatient(this);
    }

    public void removeProcedureExecution(ProcedureExecution procedureExecution) {
        this.procedureExecutions.remove(procedureExecution);
        procedureExecution.setPatient(null);
    }

    public List<ProcedureExecution> getActiveProcedureExecutions() {
        // Retorna apenas procedimentos não cancelados
        return procedureExecutions.stream()
                .filter(pe -> !"CANCELLED".equals(pe.getStatus()))
                .toList();
    }

    public long countProcedureExecutions() {
        return procedureExecutions.size();
    }

    public boolean hasProcedureExecutions() {
        return !procedureExecutions.isEmpty();
    }
}
