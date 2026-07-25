package com.tcc.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String role;
    private String fullName;

    // Doctor-specific fields
    private UUID doctorId;
    private String crm;
    private String specialty;
    private String hospitalName;

    public UserProfileResponse() {}

    // Constructor for ADMIN
    public UserProfileResponse(UUID id, String email, String role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    // Constructor for DOCTOR
    public UserProfileResponse(UUID id, String email, String role, String fullName,
                               UUID doctorId, String crm, String specialty, String hospitalName) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
        this.doctorId = doctorId;
        this.crm = crm;
        this.specialty = specialty;
        this.hospitalName = hospitalName;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    public String getCrm() { return crm; }
    public void setCrm(String crm) { this.crm = crm; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
}
