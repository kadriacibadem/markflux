package com.markflux.order.service;

import com.markflux.common.error.BusinessException;
import com.markflux.common.error.NotFoundException;
import com.markflux.common.security.AuthenticatedUser;
import com.markflux.order.domain.Order;
import com.markflux.order.domain.OrderStatus;
import com.markflux.order.messaging.OrderEventPublisher;
import com.markflux.order.repository.OrderRepository;
import com.markflux.order.web.dto.OrderResponse;
import com.markflux.order.web.dto.OrderSummaryResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public OrderResponse getOrder(AuthenticatedUser user, UUID orderId) {
        return OrderResponse.from(findOrderOf(user, orderId));
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> listOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(OrderSummaryResponse::from);
    }

    @Transactional
    public OrderResponse cancelOrder(AuthenticatedUser user, UUID orderId) {
        Order order = findOrderOf(user, orderId);
        order.cancel("Cancelled by user");
        eventPublisher.publish("OrderCancelled", order, "Cancelled by user");
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> notFound(orderId));
        switch (status) {
            case SHIPPED -> {
                order.ship();
                eventPublisher.publish("OrderShipped", order, null);
            }
            case DELIVERED -> {
                order.deliver();
                eventPublisher.publish("OrderDelivered", order, null);
            }
            default -> throw new BusinessException("INVALID_STATUS", "Status can only be SHIPPED or DELIVERED");
        }
        return OrderResponse.from(order);
    }

    private Order findOrderOf(AuthenticatedUser user, UUID orderId) {
        return orderRepository.findById(orderId)
                .filter(order -> user.isAdmin() || order.isOwnedBy(user.id()))
                .orElseThrow(() -> notFound(orderId));
    }

    private static NotFoundException notFound(UUID orderId) {
        return new NotFoundException("ORDER_NOT_FOUND", "Order not found: " + orderId);
    }
}
