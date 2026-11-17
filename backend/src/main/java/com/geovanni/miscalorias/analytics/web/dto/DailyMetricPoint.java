package com.geovanni.miscalorias.analytics.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyMetricPoint(
        LocalDate date,
        String dayLabel,
        long kcal,
        BigDecimal protein,
        boolean inGreenMargin,
        int cleanEatingPct) {
}
