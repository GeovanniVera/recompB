package com.geovanni.miscalorias.profile.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.geovanni.miscalorias.profile.domain.Sex;
import com.geovanni.miscalorias.profile.domain.UserProfile;

public record ProfileResponse(
        Long id,
        Long userId,
        Sex sex,
        int age,
        BigDecimal heightCm,
        BigDecimal currentWeightKg,
        int gymDaysPerWeek,
        BigDecimal activityFactor,
        BigDecimal deficitPct,
        BigDecimal proteinPerKg,
        BigDecimal fatPerKg,
        Instant updatedAt,
        TargetResponse targets) {

    public static ProfileResponse of(UserProfile profile, TargetResponse targets) {
        return new ProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getSex(),
                profile.getAge(),
                profile.getHeightCm(),
                profile.getCurrentWeightKg(),
                profile.getGymDaysPerWeek(),
                profile.getActivityFactor(),
                profile.getDeficitPct(),
                profile.getProteinPerKg(),
                profile.getFatPerKg(),
                profile.getUpdatedAt(),
                targets);
    }
}
