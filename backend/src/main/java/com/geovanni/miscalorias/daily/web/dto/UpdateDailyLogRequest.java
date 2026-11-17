package com.geovanni.miscalorias.daily.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Partial update of the day. {@code waterMl} sets an absolute value;
 * {@code addWaterMl} is a delta (e.g. a glass of 250 ml). Both may be sent.
 */
public record UpdateDailyLogRequest(
        @Min(value = 0, message = "El agua no puede ser negativa")
        @Max(value = 20000, message = "El agua es demasiada")
        Integer waterMl,

        @Min(value = -20000, message = "El agua a sumar está fuera de rango")
        @Max(value = 20000, message = "El agua a sumar está fuera de rango")
        Integer addWaterMl,

        Boolean isGymDay) {
}
