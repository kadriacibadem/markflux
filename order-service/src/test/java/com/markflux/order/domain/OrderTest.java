package com.markflux.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void newOrderIsPendingAndCalculatesTotal() {
        Order order = order();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("250.00");
        assertThat(order.getStatusHistory()).hasSize(1);
    }

    @Test
    void failedOrderKeepsReason() {
        Order order = order();

        order.fail("INSUFFICIENT_STOCK");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(order.getFailureReason()).isEqualTo("INSUFFICIENT_STOCK");
    }

    private static Order order() {
        return new Order(UUID.randomUUID(), UUID.randomUUID(), List.of(
                new OrderLine(UUID.randomUUID(), "Keyboard", new BigDecimal("100.00"), 2),
                new OrderLine(UUID.randomUUID(), "Mouse", new BigDecimal("50.00"), 1)), "TRY");
    }
}
