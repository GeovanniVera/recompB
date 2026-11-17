package com.geovanni.miscalorias.analytics.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Aggregates for a date range.
 *
 * <p>{@code avgKcal}, {@code avgProtein} and {@code avgCleanEatingPct} are averaged over
 * the days that actually have logged entries ({@code loggedDays}), because dividing by
 * calendar days would report a false intake of 0 for days without data.
 * {@code greenDaysPct} uses {@code totalDays} (the calendar length of the range), since
 * adherence is measured against every day you were supposed to eat well.
 */
public record PeriodStats(
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        int loggedDays,
        long avgKcal,
        BigDecimal avgProtein,
        int greenDaysCount,
        int greenDaysPct,
        int avgCleanEatingPct) {
}
