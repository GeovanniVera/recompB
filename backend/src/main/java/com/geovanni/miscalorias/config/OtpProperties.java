package com.geovanni.miscalorias.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(
        Duration ttl,
        int maxAttempts,
        Duration resendCooldown,
        int codeLength) {
}
