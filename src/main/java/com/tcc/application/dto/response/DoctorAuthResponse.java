package com.tcc.application.dto.response;

public class DoctorAuthResponse {

    private String accessToken;
    private String refreshToken;
    private String role;
    private Long doctorId;
    private String fullName;
    private String crm;
    private String email;

    public DoctorAuthResponse() {}

    public DoctorAuthResponse(String accessToken, String refreshToken, String role, Long doctorId, String fullName, String crm, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
        this.doctorId = doctorId;
        this.fullName = fullName;
        this.crm = crm;
        this.email = email;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCrm() { return crm; }
    public void setCrm(String crm) { this.crm = crm; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
