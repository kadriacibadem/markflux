package com.markflux.cart.web.dto;

import com.markflux.cart.domain.Cart;
import com.markflux.cart.domain.CartItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(UUID cartId, UUID userId, List<Item> items, int totalQuantity, BigDecimal totalAmount,
        Instant updatedAt) {

    public static CartResponse from(Cart cart) {
        List<Item> items = cart.getItems().stream().map(Item::from).toList();
        return new CartResponse(cart.getId(), cart.getUserId(), items, cart.totalQuantity(), cart.totalAmount(),
                cart.getUpdatedAt());
    }

    public static CartResponse empty(UUID userId) {
        return new CartResponse(null, userId, List.of(), 0, BigDecimal.ZERO, null);
    }

    public record Item(UUID productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal,
            boolean available) {

        static Item from(CartItem item) {
            return new Item(item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity(),
                    item.lineTotal(), item.isAvailable());
        }
    }
}
