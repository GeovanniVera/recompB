package com.geovanni.miscalorias.auth.web.dto;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        UserResponse user) {

    public static AuthResponse bearer(String accessToken, long expiresIn, Instant expiresAt, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, expiresAt, user);
    }
}
