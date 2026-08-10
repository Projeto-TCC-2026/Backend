package com.tcc.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tcc.application.dto.request.ResetPasswordRequest;
import com.tcc.application.port.out.PasswordResetPublisher;
import com.tcc.domain.model.PasswordResetToken;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.PasswordResetTokenRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.InvalidTokenException;

import jakarta.transaction.Transactional;

@Service
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

        @Value("${app.password-reset.token-expiration-minutes:15}")
        private int passwordResetTokenExpirationMinutes;

        @Value("${app.password-reset.frontend-base-url:http://localhost:4200}")
        private String frontendBaseUrl;

        private final UserRepository userRepository;
        private final PasswordResetTokenRepository passwordResetTokenRepository;
        private final PasswordEncoder passwordEncoder;
        private final PasswordResetPublisher passwordResetPublisher;

        public ForgotPasswordServiceImpl(
                        UserRepository userRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        PasswordEncoder passwordEncoder,
                        PasswordResetPublisher passwordResetPublisher) {
                this.userRepository = userRepository;
                this.passwordResetTokenRepository = passwordResetTokenRepository;
                this.passwordEncoder = passwordEncoder;
                this.passwordResetPublisher = passwordResetPublisher;
        }

        @Override
        @Transactional
        public void requestPasswordReset(String email) {

                userRepository.findByEmailAndActiveTrue(email)
                                .ifPresent(user -> {

                                        String rawToken = UUID.randomUUID().toString();
                                        String tokenHash = hashToken(rawToken);

                                        // Invalida tokens anteriores do usuário
                                        passwordResetTokenRepository.invalidateAllByUserId(user.getId());

                                        // Cria novo token de recuperação
                                        PasswordResetToken resetToken = new PasswordResetToken();

                                        resetToken.setTokenHash(tokenHash);
                                        resetToken.setUser(user);
                                        resetToken.setExpiresAt(LocalDateTime.now()
                                                        .plusMinutes(passwordResetTokenExpirationMinutes));
                                        resetToken.setUsed(false);

                                        passwordResetTokenRepository.save(resetToken);

                                        String baseUrl = frontendBaseUrl == null || frontendBaseUrl.isBlank()
                                                        ? "http://localhost:4200"
                                                        : frontendBaseUrl;

                                        try {
                                                passwordResetPublisher.publishResetRequested(
                                                                user.getEmail(),
                                                                rawToken,
                                                                baseUrl);

                                        } catch (Exception e) {

                                                // Se a publicação falhar, remove o token criado.
                                                passwordResetTokenRepository.delete(resetToken);

                                                throw new IllegalStateException(
                                                                "Falha ao publicar solicitação de recuperação de senha",
                                                                e);
                                        }
                                });
        }

        @Override
        @Transactional
        public void resetPassword(ResetPasswordRequest request) {

                PasswordResetToken resetToken = passwordResetTokenRepository
                                .findByTokenHash(hashToken(request.token()))
                                .orElseThrow(
                                                () -> new InvalidTokenException(
                                                                "Token de recuperação inválido"));

                // Token já utilizado
                if (resetToken.isUsed()) {
                        throw new InvalidTokenException(
                                        "Token de recuperação já foi usado");
                }

                // Token expirado
                if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                        throw new InvalidTokenException(
                                        "Token de recuperação expirado");
                }

                // Senhas diferentes
                if (!request.password().equals(request.passwordConfirmation())) {
                        throw new InvalidTokenException(
                                        "As senhas não conferem");
                }

                // Atualiza senha
                User user = resetToken.getUser();

                user.setPasswordHash(
                                passwordEncoder.encode(request.password()));

                userRepository.save(user);

                // Marca token como utilizado
                resetToken.setUsed(true);

                passwordResetTokenRepository.save(resetToken);
        }

        private String hashToken(String token) {

                try {

                        MessageDigest digest = MessageDigest.getInstance("SHA-256");

                        byte[] encoded = digest.digest(
                                        token.getBytes(StandardCharsets.UTF_8));

                        return HexFormat.of().formatHex(encoded);

                } catch (NoSuchAlgorithmException ex) {

                        throw new IllegalStateException(
                                        "Falha ao gerar hash do token",
                                        ex);
                }
        }
}