package com.markflux.inventory.messaging;

import com.markflux.common.messaging.Inbox;
import com.markflux.common.messaging.IncomingEvent;
import com.markflux.inventory.domain.ReservationLine;
import com.markflux.inventory.service.ReservationResult;
import com.markflux.inventory.service.StockReservationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventsListener {

    private static final String CONSUMER = "inventory-service.order-events";

    private final Inbox inbox;
    private final StockReservationService reservationService;
    private final InventoryEventPublisher eventPublisher;

    @KafkaListener(id = "inventory-order-events", topics = "order.events.v1")
    public void onMessage(ConsumerRecord<String, String> record) {
        IncomingEvent event = IncomingEvent.from(record);
        switch (event.type()) {
            case "OrderCreated" -> inbox.process(CONSUMER, event, OrderCreated.class, this::onOrderCreated);
            case "OrderConfirmed" -> inbox.process(CONSUMER, event, OrderConfirmed.class,
                    confirmed -> reservationService.confirmForOrder(confirmed.orderId()));
            case "OrderCancelled", "OrderFailed" -> inbox.process(CONSUMER, event, OrderClosed.class,
                    closed -> reservationService.releaseForOrder(closed.orderId(), closed.reason()));
            default -> {
            }
        }
    }

    private void onOrderCreated(OrderCreated event) {
        List<ReservationLine> lines = event.items().stream()
                .map(item -> new ReservationLine(item.productId(), item.quantity()))
                .toList();
        ReservationResult result = reservationService.reserveForOrder(event.orderId(), lines);
        switch (result) {
            case ReservationResult.Reserved reserved -> eventPublisher.stockReserved(reserved.reservation());
            case ReservationResult.Rejected rejected ->
                    eventPublisher.stockReservationFailed(event.orderId(), rejected.shortages());
            case ReservationResult.AlreadyExists ignored -> {
            }
        }
    }

    record OrderCreated(UUID orderId, List<Item> items) {

        record Item(UUID productId, int quantity) {
        }
    }

    record OrderConfirmed(UUID orderId) {
    }

    record OrderClosed(UUID orderId, String reason) {
    }
}
