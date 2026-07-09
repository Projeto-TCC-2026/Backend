package com.tcc.application.service;

import com.tcc.application.dto.request.LoginRequest;
import com.tcc.application.dto.request.RefreshTokenRequest;
import com.tcc.application.dto.response.AuthResponse;
import com.tcc.application.dto.response.DoctorAuthResponse;
import com.tcc.application.dto.response.PatientAuthResponse;
import com.tcc.application.dto.response.RefreshTokenResponse;
import com.tcc.application.dto.response.UserProfileResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    DoctorAuthResponse loginDoctor(LoginRequest request);

    PatientAuthResponse loginPatient(LoginRequest request);

    RefreshTokenResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    UserProfileResponse getProfile(String email);
}
