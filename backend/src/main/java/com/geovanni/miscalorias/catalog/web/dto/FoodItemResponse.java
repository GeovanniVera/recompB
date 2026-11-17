package com.geovanni.miscalorias.catalog.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code userId == null} identifies a shared 3B item, so nulls are serialized
 * explicitly here (the global Jackson config omits them).
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record FoodItemResponse(
        Long id,
        Long userId,
        String name,
        String stateTag,
        BigDecimal kcalPer100g,
        BigDecimal proteinPer100g,
        BigDecimal carbsPer100g,
        BigDecimal fatPer100g,
        List<Integer> quickGrams,
        Instant createdAt) {
}
