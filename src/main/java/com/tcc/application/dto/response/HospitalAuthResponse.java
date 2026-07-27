package com.tcc.application.dto.response;

import java.util.UUID;

public class HospitalAuthResponse {

    private String accessToken;
    private String refreshToken;
    private String role;
    private UUID hospitalId;
    private String hospitalName;
    private String email;

    public HospitalAuthResponse() {}

    public HospitalAuthResponse(String accessToken, String refreshToken, String role,
                                UUID hospitalId, String hospitalName, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.email = email;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
