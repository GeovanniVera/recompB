package com.geovanni.miscalorias.analytics.web.dto;

public record WeekComparisonResponse(
        WeekBlock base,
        WeekBlock current,
        WeekDeltas deltas) {
}
