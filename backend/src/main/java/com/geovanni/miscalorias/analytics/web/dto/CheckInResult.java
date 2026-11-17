package com.geovanni.miscalorias.analytics.web.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Nulls are meaningful: no suggestion means no deficit and no message. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CheckInResult(
        CheckInResponse checkIn,
        boolean adjustmentSuggested,
        BigDecimal suggestedDeficitPct,
        String message) {
}
