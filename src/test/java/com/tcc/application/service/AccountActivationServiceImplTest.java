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
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.tcc.application.dto.request.AccountActivationRequest;
import com.tcc.application.port.out.AccountActivationPublisher;
import com.tcc.domain.model.AccountActivationToken;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.AccountActivationTokenRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.InvalidTokenException;

@ExtendWith(MockitoExtension.class)
class AccountActivationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountActivationTokenRepository accountActivationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountActivationPublisher accountActivationPublisher;

    @InjectMocks
    private AccountActivationServiceImpl accountActivationService;

    private User user;

    private static final String BASE_URL = "http://localhost:4200";
    private static final String ENCODED_PASSWORD = "$2a$10$hashFicticioDeTeste";
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = new User("patient@test.com", "encoded", Role.PATIENT);
        user.setId(USER_ID);

        ReflectionTestUtils.setField(accountActivationService, "tokenExpirationMinutes", 4320);
        ReflectionTestUtils.setField(accountActivationService, "frontendBaseUrl", BASE_URL);
    }

    @Nested
    @DisplayName("issueActivationToken")
    class IssueActivationToken {

        @Test
        @DisplayName("deve invalidar convites pendentes e persistir apenas o hash do token")
        void shouldInvalidatePendingTokensAndPersistOnlyHash() {
            String link = accountActivationService.issueActivationToken(user, "Joao Silva");

            verify(accountActivationTokenRepository).invalidateAllByUserId(USER_ID);

            ArgumentCaptor<AccountActivationToken> captor =
                    ArgumentCaptor.forClass(AccountActivationToken.class);
            verify(accountActivationTokenRepository).save(captor.capture());

            String rawToken = link.substring(link.indexOf("token=") + "token=".length());
            AccountActivationToken saved = captor.getValue();

            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.isUsed()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
            // O token em claro só existe no link; o banco guarda o hash.
            assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
            assertThat(saved.getTokenHash()).hasSize(64);
        }

        @Test
        @DisplayName("deve remover o token quando a publicacao do e-mail falha")
        void shouldDeleteTokenWhenPublishFails() {
            doThrow(new IllegalStateException("SQS fora do ar"))
                    .when(accountActivationPublisher)
                    .publishAccountCreated(anyString(), anyString(), anyString(), anyString());

            assertThatThrownBy(() -> accountActivationService.issueActivationToken(user, "Joao Silva"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("boas-vindas");

            verify(accountActivationTokenRepository).delete(any(AccountActivationToken.class));
        }
    }

    @Nested
    @DisplayName("activateAccount")
    class ActivateAccount {

        @Test
        @DisplayName("deve definir a senha e marcar o token como usado")
        void shouldSetPasswordAndMarkTokenAsUsed() {
            AccountActivationToken token = validToken();
            when(accountActivationTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("novaSenha123")).thenReturn(ENCODED_PASSWORD);

            accountActivationService.activateAccount(
                    new AccountActivationRequest("token-cru", "novaSenha123", "novaSenha123"));

            assertThat(user.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
            assertThat(token.isUsed()).isTrue();
            verify(userRepository).save(user);
            verify(accountActivationTokenRepository).save(token);
        }

        @Test
        @DisplayName("deve lancar excecao quando o token nao existe")
        void shouldThrowWhenTokenIsInvalid() {
            when(accountActivationTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountActivationService.activateAccount(
                    new AccountActivationRequest("inexistente", "novaSenha123", "novaSenha123")))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("inválido");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando o token ja foi usado")
        void shouldThrowWhenTokenAlreadyUsed() {
            AccountActivationToken token = validToken();
            token.setUsed(true);
            when(accountActivationTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> accountActivationService.activateAccount(
                    new AccountActivationRequest("token-cru", "novaSenha123", "novaSenha123")))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("já foi usado");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando o token esta expirado")
        void shouldThrowWhenTokenIsExpired() {
            AccountActivationToken token = validToken();
            token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
            when(accountActivationTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> accountActivationService.activateAccount(
                    new AccountActivationRequest("token-cru", "novaSenha123", "novaSenha123")))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expirado");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando as senhas nao conferem")
        void shouldThrowWhenPasswordsDoNotMatch() {
            AccountActivationToken token = validToken();
            when(accountActivationTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> accountActivationService.activateAccount(
                    new AccountActivationRequest("token-cru", "novaSenha123", "outraSenha456")))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("não conferem");

            assertThat(token.isUsed()).isFalse();
            verify(userRepository, never()).save(any());
        }

        private AccountActivationToken validToken() {
            AccountActivationToken token = new AccountActivationToken();
            token.setTokenHash("hash-qualquer");
            token.setUser(user);
            token.setExpiresAt(LocalDateTime.now().plusMinutes(60));
            token.setUsed(false);
            return token;
        }
    }
}
