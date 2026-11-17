package com.geovanni.miscalorias.analytics.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.geovanni.miscalorias.analytics.domain.BiweeklyCheckIn;

/** Optional measurements (waist, note) are serialized as explicit nulls. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CheckInResponse(
        Long id,
        LocalDate checkinDate,
        BigDecimal weightKg,
        BigDecimal waistCm,
        Integer greenDaysPct,
        Integer cleanEatingPct,
        String note,
        Instant createdAt) {

    public static CheckInResponse of(BiweeklyCheckIn checkIn) {
        return new CheckInResponse(
                checkIn.getId(),
                checkIn.getCheckinDate(),
                checkIn.getWeightKg(),
                checkIn.getWaistCm(),
                checkIn.getGreenDaysPct(),
                checkIn.getCleanEatingPct(),
                checkIn.getNote(),
                checkIn.getCreatedAt());
    }
}
