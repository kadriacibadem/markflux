package com.markflux.order.web.dto;

import com.markflux.order.domain.Order;
import com.markflux.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(UUID id, OrderStatus status, BigDecimal totalAmount, String currency,
        int itemCount, Instant createdAt) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getStatus(), order.getTotalAmount(),
                order.getCurrency(), order.getItems().size(), order.getCreatedAt());
    }
}
