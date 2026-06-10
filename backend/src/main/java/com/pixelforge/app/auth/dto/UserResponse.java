package com.pixelforge.app.auth.dto;

import com.pixelforge.app.user.User;
import com.pixelforge.app.user.UserRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        UserRole role,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
