package com.geovanni.miscalorias.catalog.web.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFoodItemRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede pasar de 150 caracteres")
        String name,

        @NotNull(message = "El estado es obligatorio")
        StateTag stateTag,

        @NotNull(message = "Las kcal por 100 g son obligatorias")
        @DecimalMin(value = "0.0", message = "Las kcal por 100 g no pueden ser negativas")
        @DecimalMax(value = "2000.0", message = "Las kcal por 100 g son demasiado altas")
        @Digits(integer = 4, fraction = 2)
        BigDecimal kcalPer100g,

        @NotNull(message = "La proteína por 100 g es obligatoria")
        @DecimalMin(value = "0.0", message = "La proteína por 100 g no puede ser negativa")
        @DecimalMax(value = "1000.0", message = "La proteína por 100 g es demasiado alta")
        @Digits(integer = 4, fraction = 2)
        BigDecimal proteinPer100g,

        @NotNull(message = "Los carbohidratos por 100 g son obligatorios")
        @DecimalMin(value = "0.0", message = "Los carbohidratos por 100 g no pueden ser negativos")
        @DecimalMax(value = "1000.0", message = "Los carbohidratos por 100 g son demasiado altos")
        @Digits(integer = 4, fraction = 2)
        BigDecimal carbsPer100g,

        @NotNull(message = "La grasa por 100 g es obligatoria")
        @DecimalMin(value = "0.0", message = "La grasa por 100 g no puede ser negativa")
        @DecimalMax(value = "1000.0", message = "La grasa por 100 g es demasiado alta")
        @Digits(integer = 4, fraction = 2)
        BigDecimal fatPer100g,

        @Size(max = 10, message = "Solo puedes precargar hasta 10 porciones")
        List<@NotNull @Min(1) @Max(5000) Integer> quickGrams) {

    /**
     * The only state tags the 3B catalog understands. Serialized with its Spanish
     * label ("Cocido"), which is what the database stores and the UI shows.
     */
    public enum StateTag {
        COCIDO("Cocido"),
        CRUDO("Crudo"),
        PIEZA("Pieza"),
        DRENADO("Drenado"),
        DIRECTO("Directo");

        private final String label;

        StateTag(String label) {
            this.label = label;
        }

        @JsonValue
        public String label() {
            return label;
        }

        @JsonCreator
        public static StateTag fromLabel(String value) {
            for (StateTag tag : values()) {
                if (tag.label.equalsIgnoreCase(value) || tag.name().equalsIgnoreCase(value)) {
                    return tag;
                }
            }
            throw new IllegalArgumentException("Unknown state tag: " + value);
        }
    }
}
