package com.markflux.cart.messaging;

import com.markflux.cart.service.CartService;
import com.markflux.common.messaging.Inbox;
import com.markflux.common.messaging.IncomingEvent;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductEventsListener {

    private static final String CONSUMER = "cart-service.product-events";

    private final Inbox inbox;
    private final CartService cartService;

    @KafkaListener(id = "cart-product-events", topics = "product.events.v1")
    public void onMessage(ConsumerRecord<String, String> record) {
        IncomingEvent event = IncomingEvent.from(record);
        if (event.type().equals("ProductUpdated") || event.type().equals("ProductStatusChanged")) {
            inbox.process(CONSUMER, event, ProductChanged.class, product -> cartService.updateProduct(
                    product.productId(), product.name(), product.price(), "ACTIVE".equals(product.status())));
        }
    }

    record ProductChanged(UUID productId, String name, BigDecimal price, String status) {
    }
}
