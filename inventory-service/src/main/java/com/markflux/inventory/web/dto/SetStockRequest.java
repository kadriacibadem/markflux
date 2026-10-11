package com.markflux.inventory.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SetStockRequest(@NotNull @PositiveOrZero @Max(1_000_000) Integer quantity,
        @Size(max = 255) String reason) {
}
