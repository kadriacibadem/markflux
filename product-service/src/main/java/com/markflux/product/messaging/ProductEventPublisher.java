package com.markflux.product.messaging;

import com.markflux.common.messaging.TransactionalOutbox;
import com.markflux.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    public static final String TOPIC = "product.events.v1";

    private final TransactionalOutbox outbox;

    public void publish(String eventType, Product product) {
        outbox.save(TOPIC, product.getId().toString(), eventType, ProductEvent.from(product));
    }
}
