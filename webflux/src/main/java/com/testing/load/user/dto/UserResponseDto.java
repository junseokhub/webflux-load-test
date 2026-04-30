package com.testing.load.user.dto;

import com.testing.load.user.Role;
import com.testing.load.user.User;

import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String username,
        Role role,
        LocalDateTime createdAt
) {

    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
