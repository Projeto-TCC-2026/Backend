package com.tcc.application.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.tcc.application.dto.request.HospitalRegistrationRequest;
import com.tcc.application.dto.request.LoginRequest;
import com.tcc.application.dto.request.RefreshTokenRequest;
import com.tcc.application.dto.response.AuthResponse;
import com.tcc.application.dto.response.DoctorAuthResponse;
import com.tcc.application.dto.response.HospitalAuthResponse;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.dto.response.RefreshTokenResponse;
import com.tcc.application.mapper.HospitalMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.RefreshToken;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.RefreshTokenRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.InvalidTokenException;
import com.tcc.exception.UnauthorizedException;
import com.tcc.infrastructure.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private HospitalMapper hospitalMapper;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User doctorUser;
    private User adminUser;
    private Doctor doctor;

    private static final UUID DOCTOR_USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();
    private static final UUID PATIENT_USER_ID = UUID.randomUUID();
    private static final UUID HOSPITAL_USER_ID = UUID.randomUUID();
    private static final UUID HOSPITAL_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID TOKEN_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpiration", 604800000L);

        doctorUser = new User("doctor@test.com", "encodedPassword", Role.DOCTOR);
        doctorUser.setId(DOCTOR_USER_ID);

        adminUser = new User("admin@test.com", "encodedPassword", Role.ADMIN);
        adminUser.setId(ADMIN_USER_ID);

        Hospital hospital = new Hospital("Hospital Central", "12345678000100");
        hospital.setId(HOSPITAL_ID);

        doctor = new Doctor(doctorUser, hospital, "Dr. Carlos", "11122233344", "CRM12345");
        doctor.setId(DOCTOR_ID);
    }

    @Nested
    @DisplayName("registerHospital")
    class RegisterHospital {

        private final HospitalRegistrationRequest request = new HospitalRegistrationRequest(
                "Hospital Novo", "98765432000111", "1133334444", "contato@hospitalnovo.com",
                "Rua Nova, 100", "Sao Paulo", "SP", "senha123"
        );

        @Test
        @DisplayName("deve cadastrar hospital e usuario gestor, ja ativo")
        void shouldRegisterHospitalSuccessfully() {
            when(hospitalRepository.existsByCnpj("98765432000111")).thenReturn(false);
            when(userRepository.existsByEmail("contato@hospitalnovo.com")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("encoded");
            when(hospitalRepository.save(any(Hospital.class))).thenAnswer(i -> i.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            HospitalResponse expectedResponse = new HospitalResponse(
                    UUID.randomUUID(), "Hospital Novo", "98765432000111", "1133334444",
                    "contato@hospitalnovo.com", "Rua Nova, 100", "Sao Paulo", "SP", true, null, null);
            when(hospitalMapper.toResponse(any(Hospital.class))).thenReturn(expectedResponse);

            HospitalResponse result = authService.registerHospital(request);

            assertThat(result).isEqualTo(expectedResponse);

            ArgumentCaptor<Hospital> hospitalCaptor = ArgumentCaptor.forClass(Hospital.class);
            verify(hospitalRepository).save(hospitalCaptor.capture());
            assertThat(hospitalCaptor.getValue().getActive()).isTrue();
            assertThat(hospitalCaptor.getValue().getCnpj()).isEqualTo("98765432000111");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.HOSPITAL);
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded");
            assertThat(userCaptor.getValue().getHospital()).isEqualTo(hospitalCaptor.getValue());
        }

        @Test
        @DisplayName("deve lancar excecao quando CNPJ ja cadastrado")
        void shouldThrowWhenCnpjAlreadyExists() {
            when(hospitalRepository.existsByCnpj("98765432000111")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerHospital(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CNPJ");

            verify(hospitalRepository, never()).save(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando email ja cadastrado")
        void shouldThrowWhenEmailAlreadyExists() {
            when(hospitalRepository.existsByCnpj("98765432000111")).thenReturn(false);
            when(userRepository.existsByEmail("contato@hospitalnovo.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerHospital(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("e-mail");

            verify(hospitalRepository, never()).save(any());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("deve realizar login com sucesso")
        void shouldLoginSuccessfully() {
            LoginRequest request = new LoginRequest("doctor@test.com", "senha123");

            when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctorUser));
            when(passwordEncoder.matches("senha123", "encodedPassword")).thenReturn(true);
            when(jwtService.generateToken("doctor@test.com")).thenReturn("access-token");
            when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            AuthResponse result = authService.login(request);

            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getEmail()).isEqualTo("doctor@test.com");
            assertThat(result.getRole()).isEqualTo("DOCTOR");
        }

        @Test
        @DisplayName("deve lancar excecao quando email nao encontrado")
        void shouldThrowWhenEmailNotFound() {
            LoginRequest request = new LoginRequest("inexistente@test.com", "senha123");

            when(userRepository.findByEmail("inexistente@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Credenciais inválidas");
        }

        @Test
        @DisplayName("deve lancar excecao quando senha incorreta")
        void shouldThrowWhenWrongPassword() {
            LoginRequest request = new LoginRequest("doctor@test.com", "senhaErrada");

            when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctorUser));
            when(passwordEncoder.matches("senhaErrada", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Credenciais inválidas");
        }
    }

    @Nested
    @DisplayName("login de conta inativa")
    class LoginWithInactiveAccount {

        @Test
        @DisplayName("paciente inativado nao autentica")
        void inactivePatientCannotLogin() {
            User patientUser = new User("patient@test.com", "encodedPassword", Role.PATIENT);
            patientUser.setId(PATIENT_USER_ID);
            patientUser.setActive(false);

            LoginRequest request = new LoginRequest("patient@test.com", "senha123");
            when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(patientUser));
            when(passwordEncoder.matches("senha123", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.loginPatient(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Conta inativa");

            // Não chega a emitir token nem a consultar o perfil de paciente.
            verify(jwtService, never()).generateToken(any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("medico inativado nao autentica")
        void inactiveDoctorCannotLogin() {
            doctorUser.setActive(false);

            LoginRequest request = new LoginRequest("doctor@test.com", "senha123");
            when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctorUser));
            when(passwordEncoder.matches("senha123", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.loginDoctor(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Conta inativa");

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("conta inativa e barrada tambem no login generico")
        void inactiveAccountBlockedOnGenericLogin() {
            adminUser.setActive(false);

            LoginRequest request = new LoginRequest("admin@test.com", "senha123");
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches("senha123", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Conta inativa");
        }

        @Test
        @DisplayName("senha errada em conta inativa continua devolvendo credenciais invalidas")
        void wrongPasswordOnInactiveAccountStillReportsInvalidCredentials() {
            doctorUser.setActive(false);

            LoginRequest request = new LoginRequest("doctor@test.com", "senhaErrada");
            when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctorUser));
            when(passwordEncoder.matches("senhaErrada", "encodedPassword")).thenReturn(false);

            // A checagem de senha vem antes: não revela o estado da conta a quem
            // não sabe a credencial.
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Credenciais inválidas");
        }
    }

    @Nested
    @DisplayName("regressao: perfis ativos continuam autenticando")
    class ActiveProfilesRegression {

        @Test
        @DisplayName("ADMIN ativo autentica normalmente")
        void activeAdminStillLogs() {
            LoginRequest request = new LoginRequest("admin@test.com", "senha123");
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches("senha123", "encodedPassword")).thenReturn(true);
            when(jwtService.generateToken("admin@test.com")).thenReturn("access-token");
            when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            AuthResponse result = authService.loginAdmin(request);

            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRole()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("DOCTOR ativo autentica normalmente")
        void activeDoctorStillLogs() {
            LoginRequest request = new LoginRequest("doctor@test.com", "senha123");
            when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctorUser));
            when(passwordEncoder.matches("senha123", "encodedPassword")).thenReturn(true);
            when(doctorRepository.findByUserId(DOCTOR_USER_ID)).thenReturn(Optional.of(doctor));
            when(jwtService.generateToken("doctor@test.com")).thenReturn("access-token");
            when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            DoctorAuthResponse result = authService.loginDoctor(request);

            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getDoctorId()).isEqualTo(DOCTOR_ID);
        }

        @Test
        @DisplayName("HOSPITAL ativo autentica normalmente")
        void activeHospitalStillLogs() {
            Hospital hospital = new Hospital("Hospital Central", "12345678000100");
            hospital.setId(HOSPITAL_ID);
            User hospitalUser = new User("hospital@test.com", "encodedPassword", Role.HOSPITAL);
            hospitalUser.setId(HOSPITAL_USER_ID);
            hospitalUser.setHospital(hospital);

            LoginRequest request = new LoginRequest("hospital@test.com", "senha123");
            when(userRepository.findByEmail("hospital@test.com")).thenReturn(Optional.of(hospitalUser));
            when(passwordEncoder.matches("senha123", "encodedPassword")).thenReturn(true);
            when(jwtService.generateToken("hospital@test.com")).thenReturn("access-token");
            when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            HospitalAuthResponse result = authService.loginHospital(request);

            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getHospitalId()).isEqualTo(HOSPITAL_ID);
        }
    }

    @Nested
    @DisplayName("loginDoctor")
    class LoginDoctor {

        @Test
        @DisplayName("deve realizar login de medico com sucesso")
        void shouldLoginDoctorSuccessfully() {
            LoginRequest request = new LoginRequest("doctor@test.com", "senha123");

            when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctorUser));
            when(passwordEncoder.matches("senha123", "encodedPassword")).thenReturn(true);
            when(doctorRepository.findByUserId(DOCTOR_USER_ID)).thenReturn(Optional.of(doctor));
            when(jwtService.generateToken("doctor@test.com")).thenReturn("access-token");
            when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            DoctorAuthResponse result = authService.loginDoctor(request);

            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getDoctorId()).isEqualTo(DOCTOR_ID);
            assertThat(result.getCrm()).isEqualTo("CRM12345");
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario nao e DOCTOR")
        void shouldThrowWhenUserIsNotDoctor() {
            LoginRequest request = new LoginRequest("admin@test.com", "senha123");

            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches("senha123", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.loginDoctor(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Credenciais inválidas");
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("deve renovar token com sucesso")
        void shouldRefreshTokenSuccessfully() {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-token");

            RefreshToken existingToken = new RefreshToken(doctorUser, "valid-token", LocalDateTime.now().plusDays(7));
            existingToken.setId(TOKEN_ID);

            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(existingToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateToken("doctor@test.com")).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken()).thenReturn("new-refresh-token");

            RefreshTokenResponse result = authService.refresh(request);

            assertThat(result.getAccessToken()).isEqualTo("new-access-token");
            assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
            assertThat(existingToken.getRevoked()).isTrue();
        }

        @Test
        @DisplayName("deve lancar excecao quando refresh token invalido")
        void shouldThrowWhenTokenNotFound() {
            RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

            when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("inválido");
        }

        @Test
        @DisplayName("deve lancar excecao quando refresh token revogado")
        void shouldThrowWhenTokenRevoked() {
            RefreshTokenRequest request = new RefreshTokenRequest("revoked-token");

            RefreshToken revokedToken = new RefreshToken(doctorUser, "revoked-token", LocalDateTime.now().plusDays(7));
            revokedToken.setRevoked(true);

            when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revokedToken));

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("revogado");
        }

        @Test
        @DisplayName("deve lancar excecao quando refresh token expirado")
        void shouldThrowWhenTokenExpired() {
            RefreshTokenRequest request = new RefreshTokenRequest("expired-token");

            RefreshToken expiredToken = new RefreshToken(doctorUser, "expired-token", LocalDateTime.now().minusDays(1));

            when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expirado");
        }

        @Test
        @DisplayName("paciente inativado nao renova o access token")
        void inactivePatientCannotRefresh() {
            User patientUser = new User("patient@test.com", "encodedPassword", Role.PATIENT);
            patientUser.setId(PATIENT_USER_ID);
            patientUser.setActive(false);

            RefreshToken validToken = new RefreshToken(patientUser, "valid-token", LocalDateTime.now().plusDays(7));
            validToken.setId(TOKEN_ID);

            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(validToken));

            assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("valid-token")))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Conta inativa");

            // O token não é rotacionado nem substituído: nenhum access token novo sai.
            assertThat(validToken.getRevoked()).isFalse();
            verify(jwtService, never()).generateToken(any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("conta ativa continua renovando normalmente")
        void activeAccountStillRefreshes() {
            RefreshToken existingToken = new RefreshToken(adminUser, "valid-token", LocalDateTime.now().plusDays(7));
            existingToken.setId(TOKEN_ID);

            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(existingToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateToken("admin@test.com")).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken()).thenReturn("new-refresh-token");

            RefreshTokenResponse result = authService.refresh(new RefreshTokenRequest("valid-token"));

            assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        }
    }
}
