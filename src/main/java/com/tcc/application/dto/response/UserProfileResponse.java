package com.tcc.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private Long id;
    private String email;
    private String role;
    private String fullName;

    // Doctor-specific fields
    private Long doctorId;
    private String crm;
    private String specialty;
    private String hospitalName;

    public UserProfileResponse() {}

    // Constructor for ADMIN
    public UserProfileResponse(Long id, String email, String role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    // Constructor for DOCTOR
    public UserProfileResponse(Long id, String email, String role, String fullName,
                               Long doctorId, String crm, String specialty, String hospitalName) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
        this.doctorId = doctorId;
        this.crm = crm;
        this.specialty = specialty;
        this.hospitalName = hospitalName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getCrm() { return crm; }
    public void setCrm(String crm) { this.crm = crm; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
}
