package com.geovanni.miscalorias.shared.web;

import java.time.Duration;

import org.springframework.http.HttpStatus;

public class OtpCooldownException extends ApiException {

    private final Duration retryAfter;

    public OtpCooldownException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, "otp_cooldown",
                "Pediste un código hace poco. Espera %d segundos antes de pedir otro."
                        .formatted(Math.max(1, retryAfter.toSeconds())));
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
