package com.geovanni.miscalorias.catalog.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code userId == null} identifies a shared base plate, so nulls are serialized
 * explicitly here (the global Jackson config omits them).
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record QuickPlateResponse(
        Long id,
        Long userId,
        String title,
        String subtitle,
        BigDecimal kcal,
        BigDecimal protein,
        BigDecimal carbs,
        BigDecimal fat,
        Instant createdAt) {
}
