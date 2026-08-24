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

import com.tcc.application.dto.request.AccountActivationRequest;
import com.tcc.application.port.out.AccountActivationPublisher;
import com.tcc.domain.model.AccountActivationToken;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.AccountActivationTokenRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.InvalidTokenException;

import jakarta.transaction.Transactional;

@Service
public class AccountActivationServiceImpl implements AccountActivationService {

    @Value("${app.account-activation.token-expiration-minutes:4320}")
    private int tokenExpirationMinutes;

    @Value("${app.account-activation.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    private final UserRepository userRepository;
    private final AccountActivationTokenRepository accountActivationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountActivationPublisher accountActivationPublisher;

    public AccountActivationServiceImpl(
            UserRepository userRepository,
            AccountActivationTokenRepository accountActivationTokenRepository,
            PasswordEncoder passwordEncoder,
            AccountActivationPublisher accountActivationPublisher) {
        this.userRepository = userRepository;
        this.accountActivationTokenRepository = accountActivationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountActivationPublisher = accountActivationPublisher;
    }

    @Override
    @Transactional
    public String issueActivationToken(User user, String fullName) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        // Invalida convites anteriores ainda pendentes para o mesmo usuário.
        accountActivationTokenRepository.invalidateAllByUserId(user.getId());

        AccountActivationToken activationToken = new AccountActivationToken();
        activationToken.setTokenHash(tokenHash);
        activationToken.setUser(user);
        activationToken.setExpiresAt(LocalDateTime.now().plusMinutes(tokenExpirationMinutes));
        activationToken.setUsed(false);

        accountActivationTokenRepository.save(activationToken);

        String baseUrl = frontendBaseUrl == null || frontendBaseUrl.isBlank()
                ? "http://localhost:4200"
                : frontendBaseUrl;

        try {
            accountActivationPublisher.publishAccountCreated(user.getEmail(), fullName, rawToken, baseUrl);
        } catch (Exception e) {
            accountActivationTokenRepository.delete(activationToken);
            throw new IllegalStateException("Falha ao publicar e-mail de boas-vindas", e);
        }

        return baseUrl + "/welcome?token=" + rawToken;
    }

    @Override
    @Transactional
    public void activateAccount(AccountActivationRequest request) {
        AccountActivationToken activationToken = accountActivationTokenRepository
                .findByTokenHash(hashToken(request.token()))
                .orElseThrow(() -> new InvalidTokenException("Token de ativação inválido"));

        if (activationToken.isUsed()) {
            throw new InvalidTokenException("Token de ativação já foi usado");
        }

        if (activationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token de ativação expirado");
        }

        if (!request.password().equals(request.passwordConfirmation())) {
            throw new InvalidTokenException("As senhas não conferem");
        }

        User user = activationToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        activationToken.setUsed(true);
        accountActivationTokenRepository.save(activationToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Falha ao gerar hash do token", ex);
        }
    }
}
