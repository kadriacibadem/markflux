package com.markflux.cart.messaging;

import com.markflux.cart.service.CartService;
import com.markflux.common.messaging.Inbox;
import com.markflux.common.messaging.IncomingEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventsListener {

    private static final String CONSUMER = "cart-service.order-events";

    private final Inbox inbox;
    private final CartService cartService;

    @KafkaListener(id = "cart-order-events", topics = "order.events.v1")
    public void onMessage(ConsumerRecord<String, String> record) {
        IncomingEvent event = IncomingEvent.from(record);
        if (event.type().equals("OrderConfirmed")) {
            inbox.process(CONSUMER, event, OrderConfirmed.class, order -> cartService.removeOrderedProducts(
                    order.userId(), order.items().stream().map(OrderConfirmed.Item::productId).toList()));
        }
    }

    record OrderConfirmed(UUID orderId, UUID userId, List<Item> items) {

        record Item(UUID productId) {
        }
    }
}
