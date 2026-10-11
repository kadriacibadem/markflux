package com.markflux.inventory.domain;

import com.markflux.common.error.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Inventory(UUID productId) {
        this.productId = productId;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public int availableQuantity() {
        return quantity - reservedQuantity;
    }

    public boolean canReserve(int units) {
        return availableQuantity() >= units;
    }

    public void reserve(int units) {
        if (!canReserve(units)) {
            throw new IllegalStateException("Not enough stock for product " + productId);
        }
        reservedQuantity += units;
        touch();
    }

    public void releaseReservation(int units) {
        reservedQuantity -= units;
        touch();
    }

    public void commitReservation(int units) {
        reservedQuantity -= units;
        quantity -= units;
        touch();
    }

    public void restock(int units) {
        quantity += units;
        touch();
    }

    public int setQuantity(int newQuantity) {
        checkNotBelowReserved(newQuantity);
        int change = newQuantity - quantity;
        quantity = newQuantity;
        touch();
        return change;
    }

    public void adjust(int change) {
        checkNotBelowReserved(quantity + change);
        quantity += change;
        touch();
    }

    private void checkNotBelowReserved(int newQuantity) {
        if (newQuantity < reservedQuantity) {
            throw new BusinessException("STOCK_BELOW_RESERVED",
                    "Stock cannot be lower than the reserved quantity (" + reservedQuantity + ")");
        }
    }

    private void touch() {
        updatedAt = Instant.now();
    }
}
