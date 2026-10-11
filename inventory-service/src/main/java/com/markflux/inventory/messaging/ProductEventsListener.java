package com.markflux.inventory.messaging;

import com.markflux.common.messaging.Inbox;
import com.markflux.common.messaging.IncomingEvent;
import com.markflux.inventory.service.InventoryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductEventsListener {

    private static final String CONSUMER = "inventory-service.product-events";

    private final Inbox inbox;
    private final InventoryService inventoryService;

    @KafkaListener(id = "inventory-product-events", topics = "product.events.v1")
    public void onMessage(ConsumerRecord<String, String> record) {
        IncomingEvent event = IncomingEvent.from(record);
        if (event.type().equals("ProductCreated")) {
            inbox.process(CONSUMER, event, ProductCreated.class,
                    created -> inventoryService.addProduct(created.productId()));
        }
    }

    record ProductCreated(UUID productId) {
    }
}
