package com.markflux.order.service;

import com.markflux.order.domain.Order;
import com.markflux.order.domain.OrderStatus;
import com.markflux.order.messaging.OrderEventPublisher;
import com.markflux.order.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderSagaService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public void stockReserved(UUID orderId) {
        findPendingOrder(orderId).ifPresent(order -> {
            order.confirm();
            eventPublisher.publish("OrderConfirmed", order, null);
            log.info("Order {} confirmed", orderId);
        });
    }

    @Transactional
    public void stockReservationFailed(UUID orderId, String reason) {
        findPendingOrder(orderId).ifPresent(order -> fail(order, reason));
    }

    @Transactional
    public int failOrdersCreatedBefore(Instant createdBefore) {
        List<Order> orders = orderRepository.findPendingOrdersCreatedBefore(createdBefore);
        orders.forEach(order -> fail(order, "STOCK_RESERVATION_TIMEOUT"));
        return orders.size();
    }

    private Optional<Order> findPendingOrder(UUID orderId) {
        Optional<Order> order = orderRepository.findById(orderId);
        if (order.isEmpty() || order.get().getStatus() != OrderStatus.PENDING) {
            log.info("Ignoring inventory event for order {} because it is not pending", orderId);
            return Optional.empty();
        }
        return order;
    }

    private void fail(Order order, String reason) {
        order.fail(reason);
        eventPublisher.publish("OrderFailed", order, reason);
        log.info("Order {} failed: {}", order.getId(), reason);
    }
}
