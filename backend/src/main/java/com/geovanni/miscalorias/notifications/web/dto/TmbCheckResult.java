package com.geovanni.miscalorias.notifications.web.dto;

/**
 * Outcome of a TMB floor evaluation, also returned by the manual test endpoint.
 */
public record TmbCheckResult(
        boolean sent,
        long consumedKcal,
        long tmb,
        long missingKcal,
        String message) {
}
