package com.tcc.presentation.controller;

import com.tcc.application.dto.request.LoginRequest;
import com.tcc.application.dto.request.RefreshTokenRequest;
import com.tcc.application.dto.response.AuthResponse;
import com.tcc.application.dto.response.DoctorAuthResponse;
import com.tcc.application.dto.response.PatientAuthResponse;
import com.tcc.application.dto.response.RefreshTokenResponse;
import com.tcc.application.dto.response.UserProfileResponse;
import com.tcc.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Endpoints de autenticação")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login geral", description = "Autentica qualquer usuário e retorna access token + refresh token")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/doctor/login")
    @Operation(summary = "Login médico", description = "Autentica um médico e retorna tokens com dados do perfil")
    public ResponseEntity<DoctorAuthResponse> loginDoctor(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginDoctor(request));
    }

    @PostMapping("/patient/login")
    @Operation(summary = "Login paciente", description = "Autentica um paciente e retorna tokens com dados do perfil")
    public ResponseEntity<PatientAuthResponse> loginPatient(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginPatient(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token", description = "Gera um novo access token a partir de um refresh token válido")
    public ResponseEntity<RefreshTokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoga o refresh token, encerrando a sessão")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil do usuário autenticado", description = "Retorna os dados do usuário logado com base no token JWT")
    public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(authService.getProfile(userDetails.getUsername()));
    }
}
