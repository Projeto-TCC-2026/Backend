package com.tcc.application.dto.response;

public class PatientAuthResponse {

    private String accessToken;
    private String refreshToken;
    private String role;
    private Long patientId;
    private String fullName;
    private String email;

    public PatientAuthResponse() {}

    public PatientAuthResponse(String accessToken, String refreshToken, String role, Long patientId, String fullName, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
        this.patientId = patientId;
        this.fullName = fullName;
        this.email = email;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
