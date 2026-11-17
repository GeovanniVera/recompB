package com.geovanni.miscalorias.auth.web.dto;

/**
 * Always returned by the OTP request endpoint, whether or not the email exists,
 * so the response does not leak which accounts are registered.
 */
public record OtpRequestResponse(
        boolean sent,
        long expiresInSeconds,
        long resendAfterSeconds,
        String message) {
}
