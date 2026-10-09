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
    private String doctorPhone;

    // Hospital-specific fields
    private UUID hospitalId;
    private String phone;
    private String address;
    private String city;
    private String state;

    // Patient-specific fields
    private UUID patientId;

    public UserProfileResponse() {}

    // Constructor for ADMIN
    public UserProfileResponse(UUID id, String email, String role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    // Constructor for DOCTOR
    public UserProfileResponse(UUID id, String email, String role, String fullName,
                               UUID doctorId, String crm, String specialty, String hospitalName,
                               String doctorPhone) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
        this.doctorId = doctorId;
        this.crm = crm;
        this.specialty = specialty;
        this.hospitalName = hospitalName;
        this.doctorPhone = doctorPhone;
    }

    // Constructor for HOSPITAL
    public UserProfileResponse(UUID id, String email, String role,
                               UUID hospitalId, String hospitalName,
                               String phone, String address, String city, String state) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.state = state;
    }

    // Constructor for PATIENT
    public UserProfileResponse(UUID id, String email, String role,
                               UUID patientId, String fullName, boolean isPatient) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.patientId = patientId;
        this.fullName = fullName;
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

    public String getDoctorPhone() { return doctorPhone; }
    public void setDoctorPhone(String doctorPhone) { this.doctorPhone = doctorPhone; }

    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
}
