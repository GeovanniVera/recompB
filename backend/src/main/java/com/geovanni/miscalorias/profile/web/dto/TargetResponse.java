package com.geovanni.miscalorias.profile.web.dto;

/**
 * Goals derived from the profile on every read, never persisted.
 */
public record TargetResponse(
        long tmb,
        long tdee,
        long targetKcal,
        long minGreenKcal,
        long maxGreenKcal,
        long proteinGrams,
        long minGreenProtein,
        long carbsGrams,
        long fatGrams,
        long waterRestMl,
        long waterGymMl) {
}
