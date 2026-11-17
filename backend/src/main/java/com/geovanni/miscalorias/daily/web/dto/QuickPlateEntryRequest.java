package com.geovanni.miscalorias.daily.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record QuickPlateEntryRequest(
        @NotNull(message = "Elige un plato rápido")
        Long quickPlateId,

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "La hora debe tener formato HH:mm")
        String loggedTime) {
}
