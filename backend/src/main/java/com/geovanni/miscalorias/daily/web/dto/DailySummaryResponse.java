package com.geovanni.miscalorias.daily.web.dto;

import java.time.LocalDate;
import java.util.List;

import com.geovanni.miscalorias.profile.web.dto.TargetResponse;

/**
 * Single response shape for GET and every mutation of a day.
 */
public record DailySummaryResponse(
        LocalDate date,
        boolean isGymDay,
        int waterMl,
        long waterTargetMl,
        TargetResponse targets,
        MacroTotals consumed,
        MacroTotals remaining,
        long missingToTmb,
        boolean inGreenMargin,
        int cleanEatingPct,
        List<MealEntryResponse> entries) {
}
