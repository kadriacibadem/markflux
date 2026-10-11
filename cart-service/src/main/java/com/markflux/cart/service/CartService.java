package com.markflux.cart.service;

import com.markflux.cart.client.ProductClient;
import com.markflux.cart.domain.Cart;
import com.markflux.cart.repository.CartItemRepository;
import com.markflux.cart.repository.CartRepository;
import com.markflux.cart.web.dto.CartResponse;
import com.markflux.common.error.BusinessException;
import com.markflux.common.error.NotFoundException;
import com.markflux.common.error.ServiceUnavailableException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        return cartRepository.findByUserId(userId).map(CartResponse::from).orElseGet(() -> CartResponse.empty(userId));
    }

    public CartResponse addItem(UUID userId, UUID productId, int quantity) {
        ProductClient.Product product = findProduct(productId);
        if (!product.isActive()) {
            throw new BusinessException("PRODUCT_NOT_AVAILABLE", "Product is not on sale: " + productId);
        }
        return transactionTemplate.execute(status -> {
            cartRepository.createIfAbsent(UUID.randomUUID(), userId);
            Cart cart = lockCart(userId).orElseThrow();
            cart.addItem(productId, product.name(), product.price(), quantity);
            return CartResponse.from(cart);
        });
    }

    @Transactional
    public CartResponse changeQuantity(UUID userId, UUID productId, int quantity) {
        Cart cart = lockCart(userId).orElseThrow(() -> Cart.itemNotFound(productId));
        cart.changeQuantity(productId, quantity);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID productId) {
        Cart cart = lockCart(userId).orElseThrow(() -> Cart.itemNotFound(productId));
        cart.removeItem(productId);
        return CartResponse.from(cart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        lockCart(userId).ifPresent(Cart::clear);
    }

    @Transactional
    public void removeOrderedProducts(UUID userId, Collection<UUID> productIds) {
        lockCart(userId).ifPresent(cart -> cart.removeProducts(productIds));
    }

    @Transactional
    public void updateProduct(UUID productId, String name, BigDecimal price, boolean available) {
        cartItemRepository.updateProduct(productId, name, price, available);
    }

    private Optional<Cart> lockCart(UUID userId) {
        return cartRepository.findByUserIdForUpdate(userId);
    }

    private ProductClient.Product findProduct(UUID productId) {
        Optional<ProductClient.Product> product;
        try {
            product = productClient.getProduct(productId);
        } catch (RuntimeException ex) {
            throw new ServiceUnavailableException("PRODUCT_SERVICE_UNAVAILABLE",
                    "Product service is not available, please try again", ex);
        }
        return product.orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND",
                "Product not found: " + productId));
    }
}
