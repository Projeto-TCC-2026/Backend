package com.tcc.application.service;

import com.tcc.application.dto.request.HospitalRegistrationRequest;
import com.tcc.application.dto.request.LoginRequest;
import com.tcc.application.dto.request.RefreshTokenRequest;
import com.tcc.application.dto.response.AuthResponse;
import com.tcc.application.dto.response.DoctorAuthResponse;
import com.tcc.application.dto.response.HospitalAuthResponse;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.dto.response.PatientAuthResponse;
import com.tcc.application.dto.response.RefreshTokenResponse;
import com.tcc.application.dto.response.UserProfileResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    /** Auto-cadastro público de hospital. Cria o hospital e o usuário gestor vinculado, já ativo. */
    HospitalResponse registerHospital(HospitalRegistrationRequest request);

    DoctorAuthResponse loginDoctor(LoginRequest request);

    PatientAuthResponse loginPatient(LoginRequest request);

    AuthResponse loginAdmin(LoginRequest request);

    HospitalAuthResponse loginHospital(LoginRequest request);

    RefreshTokenResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    UserProfileResponse getProfile(String email);
}
