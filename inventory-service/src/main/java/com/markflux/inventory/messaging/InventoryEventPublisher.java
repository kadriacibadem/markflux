package com.markflux.inventory.messaging;

import com.markflux.common.messaging.TransactionalOutbox;
import com.markflux.inventory.domain.ReservationLine;
import com.markflux.inventory.domain.StockReservation;
import com.markflux.inventory.service.StockShortage;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    public static final String TOPIC = "inventory.events.v1";

    private final TransactionalOutbox outbox;

    public void stockReserved(StockReservation reservation) {
        StockReserved event = new StockReserved(reservation.getOrderId(), reservation.getId(), reservation.getLines());
        outbox.save(TOPIC, reservation.getOrderId().toString(), "StockReserved", event);
    }

    public void stockReservationFailed(UUID orderId, List<StockShortage> shortages) {
        StockReservationFailed event = new StockReservationFailed(orderId, "INSUFFICIENT_STOCK", shortages);
        outbox.save(TOPIC, orderId.toString(), "StockReservationFailed", event);
    }

    record StockReserved(UUID orderId, UUID reservationId, List<ReservationLine> items) {
    }

    record StockReservationFailed(UUID orderId, String reason, List<StockShortage> shortages) {
    }
}
