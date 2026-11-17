package com.geovanni.miscalorias.daily.web.dto;

import java.math.BigDecimal;

public record MacroTotals(
        long kcal,
        BigDecimal protein,
        BigDecimal carbs,
        BigDecimal fat) {

    public static final MacroTotals ZERO =
            new MacroTotals(0L, new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("0.0"));
}
