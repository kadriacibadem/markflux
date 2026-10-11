package com.markflux.inventory.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateReservationRequest(@NotNull UUID orderId, @NotEmpty @Size(max = 100) List<@Valid Item> items) {

    public record Item(@NotNull UUID productId, @Positive @Max(1000) int quantity) {
    }
}
