package com.geovanni.miscalorias.analytics.web.dto;

import java.time.LocalDate;
import java.util.List;

public record FourteenDayResponse(
        LocalDate startDate,
        LocalDate endDate,
        PeriodStats summary,
        List<DailyMetricPoint> points,
        PeriodStats previousWeek,
        PeriodStats currentWeek,
        WeekDeltas deltas) {
}
