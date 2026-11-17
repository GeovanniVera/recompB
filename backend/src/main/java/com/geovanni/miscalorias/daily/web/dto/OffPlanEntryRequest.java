package com.geovanni.miscalorias.daily.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Craving / off-plan food: only a label and calories, macros stay at 0.
 */
public record OffPlanEntryRequest(
        @NotBlank(message = "Escribe qué comiste")
        @Size(max = 200, message = "La descripción no puede pasar de 200 caracteres")
        String label,

        @NotNull(message = "Las kcal son obligatorias")
        @Min(value = 1, message = "Las kcal deben ser mayores a 0")
        @Max(value = 5000, message = "Las kcal son demasiado altas")
        Integer kcal,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "La hora debe tener formato HH:mm")
        String loggedTime) {
}
