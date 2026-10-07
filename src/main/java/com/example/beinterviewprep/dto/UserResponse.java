package com.example.beinterviewprep.dto;

import com.example.beinterviewprep.entity.AppUser;
import com.example.beinterviewprep.entity.Role;
import java.time.Instant;

public record UserResponse(Long id, String email, Role role, Instant createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
