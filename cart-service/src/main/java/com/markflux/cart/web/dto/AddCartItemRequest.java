package com.markflux.cart.web.dto;

import com.markflux.cart.domain.Cart;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddCartItemRequest(@NotNull UUID productId, @NotNull @Min(1) @Max(Cart.MAX_QUANTITY) Integer quantity) {
}
