package com.geovanni.miscalorias.analytics.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCheckInRequest(
        /** Defaults to today in the configured timezone when omitted. */
        LocalDate checkinDate,

        @NotNull(message = "El peso es obligatorio")
        @DecimalMin(value = "30.0", message = "El peso debe ser al menos 30 kg")
        @DecimalMax(value = "300.0", message = "El peso no puede pasar de 300 kg")
        @Digits(integer = 3, fraction = 2)
        BigDecimal weightKg,

        @DecimalMin(value = "30.0", message = "La cintura debe ser al menos 30 cm")
        @DecimalMax(value = "250.0", message = "La cintura no puede pasar de 250 cm")
        @Digits(integer = 3, fraction = 2)
        BigDecimal waistCm,

        @Size(max = 500, message = "La nota no puede pasar de 500 caracteres")
        String note) {
}
