package com.markflux.order.web.dto;

import com.markflux.order.domain.Order;
import com.markflux.order.domain.OrderItem;
import com.markflux.order.domain.OrderStatus;
import com.markflux.order.domain.OrderStatusHistory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, UUID userId, OrderStatus status, BigDecimal totalAmount, String currency,
        String failureReason, List<Item> items, List<StatusChange> statusHistory, Instant createdAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getUserId(), order.getStatus(), order.getTotalAmount(),
                order.getCurrency(), order.getFailureReason(),
                order.getItems().stream().map(Item::from).toList(),
                order.getStatusHistory().stream().map(StatusChange::from).toList(),
                order.getCreatedAt());
    }

    public record Item(UUID productId, String productName, BigDecimal unitPrice, int quantity,
            BigDecimal lineTotal) {

        static Item from(OrderItem item) {
            return new Item(item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity(),
                    item.getLineTotal());
        }
    }

    public record StatusChange(OrderStatus from, OrderStatus to, String reason, Instant changedAt) {

        static StatusChange from(OrderStatusHistory history) {
            return new StatusChange(history.getOldStatus(), history.getNewStatus(), history.getReason(),
                    history.getChangedAt());
        }
    }
}
