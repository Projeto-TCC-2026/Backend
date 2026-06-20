package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.response.UserResponse;
import com.tcc.domain.model.User;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
