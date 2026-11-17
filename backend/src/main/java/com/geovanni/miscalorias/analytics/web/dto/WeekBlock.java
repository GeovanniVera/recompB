package com.geovanni.miscalorias.analytics.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** A null {@code checkIn} means "no marker near this block", which is meaningful. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record WeekBlock(
        PeriodStats stats,
        List<DailyMetricPoint> points,
        CheckInResponse checkIn) {
}
