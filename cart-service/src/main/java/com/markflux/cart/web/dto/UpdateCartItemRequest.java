package com.markflux.cart.web.dto;

import com.markflux.cart.domain.Cart;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(@NotNull @Min(1) @Max(Cart.MAX_QUANTITY) Integer quantity) {
}
