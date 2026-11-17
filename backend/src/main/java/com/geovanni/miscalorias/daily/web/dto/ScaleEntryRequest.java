package com.geovanni.miscalorias.daily.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Entry calculated from a food item and a scale reading. */
public record ScaleEntryRequest(
        @NotNull(message = "Elige un alimento")
        Long foodItemId,

        @NotNull(message = "Los gramos son obligatorios")
        @DecimalMin(value = "1.0", message = "Los gramos deben ser al menos 1")
        @DecimalMax(value = "5000.0", message = "Los gramos son demasiados")
        @Digits(integer = 4, fraction = 2)
        BigDecimal grams,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "La hora debe tener formato HH:mm")
        String loggedTime) {
}
