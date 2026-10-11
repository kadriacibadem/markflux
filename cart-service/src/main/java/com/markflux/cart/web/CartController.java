package com.markflux.cart.web;

import com.markflux.cart.service.CartService;
import com.markflux.cart.web.dto.AddCartItemRequest;
import com.markflux.cart.web.dto.CartResponse;
import com.markflux.cart.web.dto.UpdateCartItemRequest;
import com.markflux.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(AuthenticatedUser user) {
        return cartService.getCart(user.id());
    }

    @PostMapping("/items")
    public CartResponse addItem(AuthenticatedUser user, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(user.id(), request.productId(), request.quantity());
    }

    @PatchMapping("/items/{productId}")
    public CartResponse changeQuantity(AuthenticatedUser user, @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.changeQuantity(user.id(), productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(AuthenticatedUser user, @PathVariable UUID productId) {
        return cartService.removeItem(user.id(), productId);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(AuthenticatedUser user) {
        cartService.clearCart(user.id());
        return ResponseEntity.noContent().build();
    }
}
