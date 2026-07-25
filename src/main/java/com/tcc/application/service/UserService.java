package com.tcc.application.service;

import com.tcc.application.dto.request.UserRequest;
import com.tcc.application.dto.response.UserResponse;
import com.tcc.domain.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(UserRequest request);

    Page<UserResponse> getAllUsers(Pageable pageable);
    
    Page<UserResponse> getAllUsers(Role role, Pageable pageable);

    UserResponse getUserById(UUID id);
    
    UserResponse getUserByEmail(String email);

    UserResponse updateUser(UUID id, UserRequest request);

    void deleteUser(UUID id);
    
    long countUsers();
    
    long countUsersByRole(Role role);
    
    long countInactiveUsers();
    
    long countInactiveUsersByRole(Role role);
    
    void activateUser(UUID id);
    
    void deactivateUser(UUID id);
}
