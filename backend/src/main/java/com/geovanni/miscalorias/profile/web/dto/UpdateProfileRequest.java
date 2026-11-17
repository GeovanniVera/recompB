package com.geovanni.miscalorias.profile.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Partial update: only the fields present are applied, the rest keep their value.
 */
public record UpdateProfileRequest(
        @Min(value = 14, message = "La edad debe ser al menos 14")
        @Max(value = 100, message = "La edad no puede pasar de 100")
        Integer age,

        @DecimalMin(value = "120.0", message = "La estatura debe ser al menos 120 cm")
        @DecimalMax(value = "250.0", message = "La estatura no puede pasar de 250 cm")
        BigDecimal heightCm,

        @DecimalMin(value = "35.0", message = "El peso debe ser al menos 35 kg")
        @DecimalMax(value = "300.0", message = "El peso no puede pasar de 300 kg")
        BigDecimal currentWeightKg,

        @Min(value = 0, message = "Los días de gym deben ser al menos 0")
        @Max(value = 7, message = "Los días de gym no pueden pasar de 7")
        Integer gymDaysPerWeek,

        @DecimalMin(value = "0.03", message = "El déficit debe ser al menos 3%")
        @DecimalMax(value = "0.40", message = "El déficit no puede pasar de 40%")
        BigDecimal deficitPct) {
}
