package com.geovanni.miscalorias.analytics.web.dto;

import java.math.BigDecimal;

public record WeekDeltas(
        long kcalDelta,
        BigDecimal proteinDelta,
        int greenPctDelta,
        int cleanPctDelta) {
}
