package com.tcc.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.tcc.application.service.AccountAccessService;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountAccessService accountAccessService;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void rejectsExistingTokensForAnInactiveProfile() {
        User hospitalUser = new User("hospital@test.com", "hash", Role.HOSPITAL);
        hospitalUser.setId(UUID.randomUUID());
        when(userRepository.findByEmail("hospital@test.com")).thenReturn(Optional.of(hospitalUser));
        when(accountAccessService.isAccountActive(hospitalUser)).thenReturn(false);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("hospital@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
