package com.markflux.inventory.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustStockRequest(@NotNull @Min(-1_000_000) @Max(1_000_000) Integer quantityChange,
        @NotBlank @Size(max = 255) String reason) {
}
