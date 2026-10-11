package com.markflux.order.domain;

import com.markflux.common.error.ConflictException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, updatable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false, updatable = false)
    private String currency;

    @Column(name = "failure_reason")
    private String failureReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("productName")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("changedAt")
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Order(UUID userId, UUID idempotencyKey, List<OrderLine> lines, String currency) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.currency = currency;
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
        for (OrderLine line : lines) {
            items.add(new OrderItem(this, line));
            totalAmount = totalAmount.add(line.lineTotal());
        }
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
        statusHistory.add(new OrderStatusHistory(this, null, OrderStatus.PENDING, "Order placed"));
    }

    public void confirm() {
        changeStatus(OrderStatus.CONFIRMED, "Stock reserved");
    }

    public void fail(String reason) {
        changeStatus(OrderStatus.FAILED, reason);
        failureReason = reason;
    }

    public void cancel(String reason) {
        changeStatus(OrderStatus.CANCELLED, reason);
    }

    public void ship() {
        changeStatus(OrderStatus.SHIPPED, "Shipped");
    }

    public void deliver() {
        changeStatus(OrderStatus.DELIVERED, "Delivered");
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return userId.equals(candidateUserId);
    }

    private void changeStatus(OrderStatus newStatus, String reason) {
        if (!status.canChangeTo(newStatus)) {
            throw new ConflictException("INVALID_ORDER_STATE",
                    "Order cannot move from " + status + " to " + newStatus);
        }
        statusHistory.add(new OrderStatusHistory(this, status, newStatus, reason));
        status = newStatus;
        updatedAt = Instant.now();
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    public List<OrderStatusHistory> getStatusHistory() {
        return List.copyOf(statusHistory);
    }
}
