package com.tcc.application.service;

import com.tcc.application.dto.request.UserRequest;
import com.tcc.application.dto.response.UserResponse;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserRequest request;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID NONEXISTENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = new User("admin@test.com", "encodedPassword", Role.ADMIN);
        user.setId(USER_ID);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        request = new UserRequest("admin@test.com", "senha123", "ADMIN", null);
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("deve criar usuario com sucesso")
        void shouldCreateUserSuccessfully() {
            when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);

            UserResponse result = userService.createUser(request);

            assertThat(result.email()).isEqualTo("admin@test.com");
            assertThat(result.role()).isEqualTo("ADMIN");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("deve lancar excecao quando email duplicado")
        void shouldThrowWhenDuplicateEmail() {
            when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("e-mail");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando role invalida")
        void shouldThrowWhenInvalidRole() {
            UserRequest invalidRequest = new UserRequest("user@test.com", "senha123", "INVALID_ROLE", null);
            when(userRepository.existsByEmail("user@test.com")).thenReturn(false);

            assertThatThrownBy(() -> userService.createUser(invalidRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Role inválida");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("deve inativar usuario com sucesso (soft delete)")
        void shouldSoftDeleteUserSuccessfully() {
            when(userRepository.findByIdAndActiveTrue(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);

            userService.deleteUser(USER_ID);

            assertThat(user.getActive()).isFalse();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario nao encontrado")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByIdAndActiveTrue(NONEXISTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(NONEXISTENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("deve lancar excecao quando email duplicado em update")
        void shouldThrowWhenDuplicateEmailOnUpdate() {
            User existingUser = new User("old@test.com", "encoded", Role.ADMIN);
            existingUser.setId(USER_ID);

            UserRequest updateRequest = new UserRequest("taken@test.com", "senha123", "ADMIN", null);

            when(userRepository.findByIdAndActiveTrue(USER_ID)).thenReturn(Optional.of(existingUser));
            when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(USER_ID, updateRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("e-mail");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando role invalida em update")
        void shouldThrowWhenInvalidRoleOnUpdate() {
            User existingUser = new User("user@test.com", "encoded", Role.ADMIN);
            existingUser.setId(USER_ID);

            UserRequest updateRequest = new UserRequest("user@test.com", "senha123", "INVALID", null);

            when(userRepository.findByIdAndActiveTrue(USER_ID)).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> userService.updateUser(USER_ID, updateRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Role inválida");
        }
    }
}
