package com.tcc.application.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.tcc.application.dto.request.ResetPasswordRequest;
import com.tcc.application.port.out.PasswordResetPublisher;
import com.tcc.domain.model.PasswordResetToken;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.PasswordResetTokenRepository;
import com.tcc.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetPublisher passwordResetPublisher;

    @InjectMocks
    private ForgotPasswordServiceImpl forgotPasswordService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                forgotPasswordService,
                "passwordResetTokenExpirationMinutes",
                15);

        ReflectionTestUtils.setField(
                forgotPasswordService,
                "frontendBaseUrl",
                "http://localhost:4200");

        user = new User(
                "doctor@test.com",
                "encodedPassword",
                Role.DOCTOR);

        user.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("deve criar token de recuperação para usuário existente")
    void shouldCreatePasswordResetTokenForExistingUser() {

        when(userRepository.findByEmailAndActiveTrue("doctor@test.com"))
                .thenReturn(Optional.of(user));

        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertThatCode(() -> forgotPasswordService.requestPasswordReset(
                "doctor@test.com")).doesNotThrowAnyException();

        verify(passwordResetTokenRepository)
                .save(any(PasswordResetToken.class));

        verify(passwordResetPublisher)
                .publishResetRequested(
                        anyString(),
                        anyString(),
                        anyString());
    }

    @Test
    @DisplayName("deve ignorar solicitação quando usuário não existe")
    void shouldIgnoreResetRequestWhenUserDoesNotExist() {

        when(userRepository.findByEmailAndActiveTrue("inexistente@test.com"))
                .thenReturn(Optional.empty());

        assertThatCode(() -> forgotPasswordService.requestPasswordReset(
                "inexistente@test.com")).doesNotThrowAnyException();

        verify(passwordResetTokenRepository, never())
                .save(any(PasswordResetToken.class));

        verify(passwordResetPublisher, never())
                .publishResetRequested(
                        anyString(),
                        anyString(),
                        anyString());
    }

    @Test
    @DisplayName("deve atualizar a senha quando o token for válido")
    void shouldResetPasswordWhenTokenIsValid() {

        String rawToken = "raw-token";

        PasswordResetToken resetToken = new PasswordResetToken();

        resetToken.setTokenHash(
                "dummy-hash");

        resetToken.setUser(user);

        resetToken.setExpiresAt(
                LocalDateTime.now().plusMinutes(10));

        resetToken.setUsed(false);

        when(passwordResetTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(resetToken));

        when(passwordEncoder.encode("novaSenha123"))
                .thenReturn("encoded-new-password");

        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertThatCode(() -> forgotPasswordService.resetPassword(
                new ResetPasswordRequest(
                        rawToken,
                        "novaSenha123",
                        "novaSenha123")))
                .doesNotThrowAnyException();

        verify(userRepository)
                .save(user);

        verify(passwordResetTokenRepository)
                .save(any(PasswordResetToken.class));
    }

}
