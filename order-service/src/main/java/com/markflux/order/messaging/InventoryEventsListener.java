package com.markflux.order.messaging;

import com.markflux.common.messaging.Inbox;
import com.markflux.common.messaging.IncomingEvent;
import com.markflux.order.service.OrderSagaService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventsListener {

    private static final String CONSUMER = "order-service.inventory-events";

    private final Inbox inbox;
    private final OrderSagaService sagaService;

    @KafkaListener(id = "order-inventory-events", topics = "inventory.events.v1")
    public void onMessage(ConsumerRecord<String, String> record) {
        IncomingEvent event = IncomingEvent.from(record);
        switch (event.type()) {
            case "StockReserved" -> inbox.process(CONSUMER, event, StockReserved.class,
                    reserved -> sagaService.stockReserved(reserved.orderId()));
            case "StockReservationFailed" -> inbox.process(CONSUMER, event, StockReservationFailed.class,
                    failed -> sagaService.stockReservationFailed(failed.orderId(), failed.reason()));
            default -> {
            }
        }
    }

    record StockReserved(UUID orderId) {
    }

    record StockReservationFailed(UUID orderId, String reason) {
    }
}
