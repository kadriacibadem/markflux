package com.markflux.order.domain;

import java.util.Set;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED;

    public boolean canChangeTo(OrderStatus target) {
        Set<OrderStatus> allowed = switch (this) {
            case PENDING -> Set.of(CONFIRMED, FAILED, CANCELLED);
            case CONFIRMED -> Set.of(SHIPPED, CANCELLED);
            case SHIPPED -> Set.of(DELIVERED);
            case DELIVERED, CANCELLED, FAILED -> Set.of();
        };
        return allowed.contains(target);
    }
}
