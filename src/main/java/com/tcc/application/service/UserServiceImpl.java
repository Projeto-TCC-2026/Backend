package com.tcc.application.service;

import com.tcc.application.dto.request.UserRequest;
import com.tcc.application.dto.response.UserResponse;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorMessages.duplicateUserEmail(request.email()));
        }

        Role role = parseRole(request.role());

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                role
        );

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAllByActiveTrue(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Role role, Pageable pageable) {
        if (role == null) {
            return getAllUsers(pageable);
        }
        return userRepository.findAllByRoleAndActiveTrue(role, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundById(id)));
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundByEmail(email)));
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UserRequest request) {
        User user = userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundById(id)));

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorMessages.duplicateUserEmail(request.email()));
        }

        validateRole(request.role());

        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(parseRole(request.role()));

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundById(id)));

        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.countByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersByRole(Role role) {
        return userRepository.countByRoleAndActiveTrue(role);
    }

    @Override
    @Transactional(readOnly = true)
    public long countInactiveUsers() {
        return userRepository.countByActiveFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public long countInactiveUsersByRole(Role role) {
        return userRepository.countByRoleAndActiveFalse(role);
    }

    @Override
    @Transactional
    public void activateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundById(id)));
        
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        User user = userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundById(id)));
        
        user.setActive(false);
        userRepository.save(user);
    }

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorMessages.INVALID_ROLE);
        }
    }

    private void validateRole(String role) {
        parseRole(role);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
