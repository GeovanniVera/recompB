package com.geovanni.miscalorias.auth.web.dto;

import java.time.Instant;

import com.geovanni.miscalorias.auth.domain.AppUser;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        boolean telegramLinked,
        Instant createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getTelegramChatId() != null,
                user.getCreatedAt());
    }
}
