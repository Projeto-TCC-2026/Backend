package com.tcc.presentation.controller;

import com.tcc.application.dto.request.ChangePasswordRequest;
import com.tcc.application.dto.request.HospitalRegistrationRequest;
import com.tcc.application.dto.request.LoginRequest;
import com.tcc.application.dto.request.RefreshTokenRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.AuthResponse;
import com.tcc.application.dto.response.DoctorAuthResponse;
import com.tcc.application.dto.response.HospitalAuthResponse;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.dto.response.PatientAuthResponse;
import com.tcc.application.dto.response.RefreshTokenResponse;
import com.tcc.application.dto.response.UserProfileResponse;
import com.tcc.application.service.AuthService;
import com.tcc.application.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Endpoints de autenticação")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login geral", description = "Autentica qualquer usuário e retorna access token + refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/doctor/login")
    @Operation(summary = "Login médico", description = "Autentica um médico e retorna tokens com dados do perfil")
    public ResponseEntity<ApiResponse<DoctorAuthResponse>> loginDoctor(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginDoctor(request)));
    }

    @PostMapping("/patient/login")
    @Operation(summary = "Login paciente", description = "Autentica um paciente e retorna tokens com dados do perfil")
    public ResponseEntity<ApiResponse<PatientAuthResponse>> loginPatient(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginPatient(request)));
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Login administrador", description = "Autentica um administrador e retorna tokens com dados do perfil")
    public ResponseEntity<AuthResponse> loginAdmin(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginAdmin(request));
    }

    @PostMapping("/hospital/login")
    @Operation(summary = "Login hospital", description = "Autentica um gestor de hospital e retorna tokens com dados do hospital")
    public ResponseEntity<ApiResponse<HospitalAuthResponse>> loginHospital(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginHospital(request)));
    }

    @PostMapping("/register/hospital")
    @Operation(
        summary = "Auto-cadastro de hospital",
        description = "Endpoint público para hospitais se auto-cadastrarem na plataforma. " +
                      "Cria o hospital e o usuário gestor vinculado, já ativos e prontos para login."
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> registerHospital(
            @Valid @RequestBody HospitalRegistrationRequest request) {
        HospitalResponse hospital = authService.registerHospital(request);
        ApiResponse<HospitalResponse> response = ApiResponse.success(hospital,
                "Cadastro realizado com sucesso. Você já pode fazer login.");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token", description = "Gera um novo access token a partir de um refresh token válido")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoga o refresh token, encerrando a sessão")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil do usuário autenticado", description = "Retorna os dados do usuário logado com base no token JWT")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(authService.getProfile(userDetails.getUsername())));
    }

    @PatchMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Alterar senha", description = "Permite ao usuário autenticado alterar a própria senha informando a senha atual")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Senha alterada com sucesso."));
    }
}
