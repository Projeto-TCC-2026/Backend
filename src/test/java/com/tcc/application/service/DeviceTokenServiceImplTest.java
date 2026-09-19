package com.tcc.application.service;

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

import com.tcc.application.dto.request.DeviceTokenRequest;
import com.tcc.domain.model.DeviceToken;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DeviceTokenRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceImplTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeviceTokenServiceImpl deviceTokenService;

    private static final String EMAIL = "patient@tcc.com";
    private static final String TOKEN = "fcm-token-abc";
    private static final String PLATFORM = "ANDROID";
    private static final String DEVICE_ID = "device-001";

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(EMAIL, "hash", Role.PATIENT);
        user.setId(UUID.randomUUID());
    }

    private DeviceTokenRequest requestOf(String token, String platform, String deviceId) {
        return new DeviceTokenRequest(token, platform, deviceId);
    }

    @Nested
    @DisplayName("registro de token")
    class Register {

        @Test
        @DisplayName("deve criar novo registro quando o token ainda nao existe")
        void shouldCreateNewRecordWhenTokenDoesNotExist() {
            when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(user));
            when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

            deviceTokenService.register(EMAIL, requestOf(TOKEN, PLATFORM, DEVICE_ID));

            ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
            verify(deviceTokenRepository).save(captor.capture());

            DeviceToken saved = captor.getValue();
            assertThat(saved.getId()).isNull();
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getToken()).isEqualTo(TOKEN);
            assertThat(saved.getPlatform()).isEqualTo(PLATFORM);
            assertThat(saved.getDeviceId()).isEqualTo(DEVICE_ID);
        }

        @Test
        @DisplayName("deve aceitar registro sem identificador de dispositivo")
        void shouldAcceptRegistrationWithoutDeviceId() {
            when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(user));
            when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

            deviceTokenService.register(EMAIL, requestOf(TOKEN, PLATFORM, null));

            ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
            verify(deviceTokenRepository).save(captor.capture());
            assertThat(captor.getValue().getDeviceId()).isNull();
        }

        @Test
        @DisplayName("deve reaproveitar o registro existente quando o token ja esta cadastrado")
        void shouldReuseExistingRecordWhenTokenAlreadyExists() {
            DeviceToken existing = new DeviceToken(user, TOKEN, "IOS", "device-old");
            existing.setId(UUID.randomUUID());

            when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(user));
            when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(existing));

            deviceTokenService.register(EMAIL, requestOf(TOKEN, PLATFORM, DEVICE_ID));

            ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
            verify(deviceTokenRepository).save(captor.capture());

            DeviceToken saved = captor.getValue();
            assertThat(saved).isSameAs(existing);
            assertThat(saved.getPlatform()).isEqualTo(PLATFORM);
            assertThat(saved.getDeviceId()).isEqualTo(DEVICE_ID);
        }

        @Test
        @DisplayName("deve revincular o token ao usuario autenticado quando pertencia a outro usuario")
        void shouldRebindTokenToAuthenticatedUser() {
            User previousOwner = new User("outro@tcc.com", "hash", Role.PATIENT);
            previousOwner.setId(UUID.randomUUID());
            DeviceToken existing = new DeviceToken(previousOwner, TOKEN, PLATFORM, DEVICE_ID);
            existing.setId(UUID.randomUUID());

            when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(user));
            when(deviceTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(existing));

            deviceTokenService.register(EMAIL, requestOf(TOKEN, PLATFORM, DEVICE_ID));

            ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
            verify(deviceTokenRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(user);
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario autenticado nao esta ativo ou nao existe")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.empty());

            DeviceTokenRequest request = requestOf(TOKEN, PLATFORM, DEVICE_ID);

            assertThatThrownBy(() -> deviceTokenService.register(EMAIL, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuário");

            verify(deviceTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("remocao de token")
    class Unregister {

        @Test
        @DisplayName("deve remover o token do usuario autenticado")
        void shouldDeleteTokenOfAuthenticatedUser() {
            DeviceToken existing = new DeviceToken(user, TOKEN, PLATFORM, DEVICE_ID);
            existing.setId(UUID.randomUUID());

            when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(user));
            when(deviceTokenRepository.findByTokenAndUserId(TOKEN, user.getId()))
                    .thenReturn(Optional.of(existing));

            deviceTokenService.unregister(EMAIL, TOKEN);

            verify(deviceTokenRepository).delete(existing);
        }

        @Test
        @DisplayName("deve lancar excecao quando o token nao pertence ao usuario autenticado")
        void shouldThrowWhenTokenBelongsToAnotherUser() {
            when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(user));
            when(deviceTokenRepository.findByTokenAndUserId(TOKEN, user.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> deviceTokenService.unregister(EMAIL, TOKEN))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Token de dispositivo");

            verify(deviceTokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario autenticado nao esta ativo ou nao existe")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deviceTokenService.unregister(EMAIL, TOKEN))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuário");

            verify(deviceTokenRepository, never()).delete(any());
        }
    }
}
