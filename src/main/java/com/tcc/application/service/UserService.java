package com.tcc.application.service;

import com.tcc.application.dto.request.UserRequest;
import com.tcc.application.dto.response.UserResponse;
import com.tcc.domain.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse createUser(UserRequest request);

    Page<UserResponse> getAllUsers(Pageable pageable);
    
    Page<UserResponse> getAllUsers(Role role, Pageable pageable);

    UserResponse getUserById(Long id);
    
    UserResponse getUserByEmail(String email);

    UserResponse updateUser(Long id, UserRequest request);

    void deleteUser(Long id);
    
    // Métodos administrativos
    long countUsers();
    
    long countUsersByRole(Role role);
    
    long countInactiveUsers();
    
    long countInactiveUsersByRole(Role role);
    
    void activateUser(Long id);
    
    void deactivateUser(Long id);
}
