package com.markflux.cart.domain;

import com.markflux.common.error.BusinessException;
import com.markflux.common.error.NotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    public static final int MAX_ITEMS = 50;
    public static final int MAX_QUANTITY = 99;

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("addedAt")
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Cart(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void addItem(UUID productId, String productName, BigDecimal price, int quantity) {
        Optional<CartItem> existing = findItem(productId);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            checkQuantity(item.getQuantity() + quantity);
            item.updateProduct(productName, price, true);
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            if (items.size() >= MAX_ITEMS) {
                throw new BusinessException("CART_FULL", "A cart can contain at most " + MAX_ITEMS + " products");
            }
            checkQuantity(quantity);
            items.add(new CartItem(this, productId, productName, price, quantity));
        }
        updatedAt = Instant.now();
    }

    public void changeQuantity(UUID productId, int quantity) {
        checkQuantity(quantity);
        CartItem item = findItem(productId).orElseThrow(() -> itemNotFound(productId));
        item.setQuantity(quantity);
        updatedAt = Instant.now();
    }

    public void removeItem(UUID productId) {
        CartItem item = findItem(productId).orElseThrow(() -> itemNotFound(productId));
        items.remove(item);
        updatedAt = Instant.now();
    }

    public void removeProducts(Collection<UUID> productIds) {
        if (items.removeIf(item -> productIds.contains(item.getProductId()))) {
            updatedAt = Instant.now();
        }
    }

    public void clear() {
        items.clear();
        updatedAt = Instant.now();
    }

    public BigDecimal totalAmount() {
        return items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int totalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public static NotFoundException itemNotFound(UUID productId) {
        return new NotFoundException("CART_ITEM_NOT_FOUND", "Product is not in the cart: " + productId);
    }

    private Optional<CartItem> findItem(UUID productId) {
        return items.stream().filter(item -> item.getProductId().equals(productId)).findFirst();
    }

    private static void checkQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new BusinessException("INVALID_QUANTITY", "Quantity must be between 1 and " + MAX_QUANTITY);
        }
    }

    public List<CartItem> getItems() {
        return List.copyOf(items);
    }
}
