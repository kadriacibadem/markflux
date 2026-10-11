package com.markflux.order.messaging;

import com.markflux.common.messaging.TransactionalOutbox;
import com.markflux.order.domain.Order;
import com.markflux.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    public static final String TOPIC = "order.events.v1";

    private final TransactionalOutbox outbox;

    public void publish(String eventType, Order order, String reason) {
        List<OrderEvent.Item> items = order.getItems().stream()
                .map(item -> new OrderEvent.Item(item.getProductId(), item.getProductName(), item.getQuantity(),
                        item.getUnitPrice()))
                .toList();
        OrderEvent event = new OrderEvent(order.getId(), order.getUserId(), order.getStatus(), items,
                order.getTotalAmount(), order.getCurrency(), reason);
        outbox.save(TOPIC, order.getId().toString(), eventType, event);
    }

    record OrderEvent(UUID orderId, UUID userId, OrderStatus status, List<Item> items, BigDecimal totalAmount,
            String currency, String reason) {

        record Item(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
        }
    }
}
