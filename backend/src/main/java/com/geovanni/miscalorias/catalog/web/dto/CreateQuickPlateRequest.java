package com.geovanni.miscalorias.catalog.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuickPlateRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 120, message = "El título no puede pasar de 120 caracteres")
        String title,

        @Size(max = 255, message = "El subtítulo no puede pasar de 255 caracteres")
        String subtitle,

        @NotNull(message = "Las kcal son obligatorias")
        @DecimalMin(value = "0.0", message = "Las kcal deben ser mayores a 0")
        @DecimalMax(value = "10000.0", message = "Las kcal son demasiado altas")
        @Digits(integer = 6, fraction = 2)
        BigDecimal kcal,

        @NotNull(message = "La proteína es obligatoria")
        @DecimalMin(value = "0.0", message = "La proteína no puede ser negativa")
        @DecimalMax(value = "1000.0", message = "La proteína es demasiado alta")
        @Digits(integer = 4, fraction = 2)
        BigDecimal protein,

        @NotNull(message = "Los carbohidratos son obligatorios")
        @DecimalMin(value = "0.0", message = "Los carbohidratos no pueden ser negativos")
        @DecimalMax(value = "1000.0", message = "Los carbohidratos son demasiado altos")
        @Digits(integer = 4, fraction = 2)
        BigDecimal carbs,

        @NotNull(message = "La grasa es obligatoria")
        @DecimalMin(value = "0.0", message = "La grasa no puede ser negativa")
        @DecimalMax(value = "1000.0", message = "La grasa es demasiado alta")
        @Digits(integer = 4, fraction = 2)
        BigDecimal fat) {
}
