package com.geovanni.miscalorias.daily.web.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.geovanni.miscalorias.daily.domain.MealEntry;

/**
 * Nulls are meaningful here (a quick-plate entry has no {@code grams} and no
 * {@code foodItemId}), so they are serialized explicitly instead of being dropped
 * by the global {@code non_null} policy.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record MealEntryResponse(
        Long id,
        String entryName,
        Long foodItemId,
        BigDecimal grams,
        long kcal,
        BigDecimal protein,
        BigDecimal carbs,
        BigDecimal fat,
        boolean isOffPlan,
        String loggedTime) {

    public static MealEntryResponse of(MealEntry entry) {
        return new MealEntryResponse(
                entry.getId(),
                entry.getEntryName(),
                entry.getFoodItemId(),
                entry.getGrams(),
                entry.getKcal().setScale(0, java.math.RoundingMode.HALF_UP).longValueExact(),
                entry.getProtein(),
                entry.getCarbs(),
                entry.getFat(),
                entry.isOffPlan(),
                entry.getLoggedTime());
    }
}
