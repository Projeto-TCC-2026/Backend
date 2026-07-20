package com.tcc.application.service;

import com.tcc.application.dto.request.LoginRequest;
import com.tcc.application.dto.request.RefreshTokenRequest;
import com.tcc.application.dto.response.AuthResponse;
import com.tcc.application.dto.response.DoctorAuthResponse;
import com.tcc.application.dto.response.PatientAuthResponse;
import com.tcc.application.dto.response.RefreshTokenResponse;
import com.tcc.application.dto.response.UserProfileResponse;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.RefreshToken;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.RefreshTokenRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.InvalidTokenException;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;
import com.tcc.infrastructure.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           DoctorRepository doctorRepository,
                           PatientRepository patientRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = findAndValidateUser(request);
        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = createRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getRole().name());
    }

    @Override
    @Transactional
    public DoctorAuthResponse loginDoctor(LoginRequest request) {
        User user = findAndValidateUser(request);

        if (user.getRole() != Role.DOCTOR) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        Doctor doctor = doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de médico não encontrado"));

        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = createRefreshToken(user);

        return new DoctorAuthResponse(
                accessToken, refreshToken, user.getRole().name(),
                doctor.getId(), doctor.getFullName(), doctor.getCrm(), user.getEmail()
        );
    }

    @Override
    @Transactional
    public PatientAuthResponse loginPatient(LoginRequest request) {
        User user = findAndValidateUser(request);

        if (user.getRole() != Role.PATIENT) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de paciente não encontrado"));

        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = createRefreshToken(user);

        return new PatientAuthResponse(
                accessToken, refreshToken, user.getRole().name(),
                patient.getId(), patient.getFullName(), user.getEmail()
        );
    }

    @Override
    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token inválido"));

        if (refreshToken.getRevoked()) {
            throw new InvalidTokenException("Refresh token já foi revogado");
        }

        if (refreshToken.isExpired()) {
            throw new InvalidTokenException("Refresh token expirado");
        }

        // Rotaciona — invalida o atual e gera um novo
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newAccessToken = jwtService.generateToken(refreshToken.getUser().getEmail());
        String newRefreshToken = createRefreshToken(refreshToken.getUser());

        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token inválido"));

        refreshTokenRepository.revokeAllByUserId(refreshToken.getUser().getId());
    }

    @Override
    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (user.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Perfil de médico não encontrado"));

            String hospitalName = doctor.getHospital() != null ? doctor.getHospital().getName() : null;

            return new UserProfileResponse(
                    user.getId(), user.getEmail(), user.getRole().name(), doctor.getFullName(),
                    doctor.getId(), doctor.getCrm(), doctor.getSpecialty(), hospitalName
            );
        }

        // ADMIN or other roles — base fields only
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getRole().name());
    }

    // --- Helpers ---

    private User findAndValidateUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        return user;
    }

    private String createRefreshToken(User user) {
        refreshTokenRepository.revokeAllByUserId(user.getId());

        String tokenValue = jwtService.generateRefreshToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshExpiration / 1000);

        RefreshToken refreshToken = new RefreshToken(user, tokenValue, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return tokenValue;
    }
}
