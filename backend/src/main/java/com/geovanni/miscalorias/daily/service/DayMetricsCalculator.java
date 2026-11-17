package com.geovanni.miscalorias.daily.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

import com.geovanni.miscalorias.daily.domain.MealEntry;
import com.geovanni.miscalorias.profile.web.dto.TargetResponse;

/**
 * Pure day-level math shared by the daily log and analytics slices, so
 * "green margin", "clean eating" and "missing to TMB" are defined in exactly
 * one place.
 */
public final class DayMetricsCalculator {

    private DayMetricsCalculator() {
    }

    /** Entry kcal as shown to the client (whole numbers). */
    public static long entryKcal(MealEntry entry) {
        return entry.getKcal().setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static long consumedKcal(List<MealEntry> entries) {
        return entries.stream().mapToLong(DayMetricsCalculator::entryKcal).sum();
    }

    public static BigDecimal consumedMacro(List<MealEntry> entries, Function<MealEntry, BigDecimal> extractor) {
        return entries.stream()
                .map(extractor)
                .reduce(new BigDecimal("0.0"), BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * Share of calories that did not come from off-plan entries. A day with no
     * consumption counts as 100% clean, matching the daily summary contract.
     */
    public static int cleanEatingPct(List<MealEntry> entries) {
        double totalKcal = entries.stream().mapToDouble(entry -> entry.getKcal().doubleValue()).sum();
        if (totalKcal <= 0) {
            return 100;
        }
        double cleanKcal = entries.stream()
                .filter(entry -> !entry.isOffPlan())
                .mapToDouble(entry -> entry.getKcal().doubleValue())
                .sum();
        return (int) Math.round(cleanKcal * 100.0 / totalKcal);
    }

    /** Inside the +/-5% band AND at least 90% of the protein target. */
    public static boolean inGreenMargin(long consumedKcal, BigDecimal consumedProtein, TargetResponse targets) {
        return consumedKcal >= targets.minGreenKcal()
                && consumedKcal <= targets.maxGreenKcal()
                && consumedProtein.compareTo(BigDecimal.valueOf(targets.minGreenProtein())) >= 0;
    }

    public static long missingToTmb(long tmb, long consumedKcal) {
        return Math.max(0L, tmb - consumedKcal);
    }

    public static BigDecimal scale1(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
    }
}
