package com.markflux.inventory.web.dto;

import com.markflux.inventory.domain.ReservationLine;
import com.markflux.inventory.domain.ReservationStatus;
import com.markflux.inventory.domain.StockReservation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(UUID id, UUID orderId, ReservationStatus status, List<ReservationLine> items,
        Instant expiresAt, String reason, Instant createdAt) {

    public static ReservationResponse from(StockReservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getOrderId(), reservation.getStatus(),
                reservation.getLines(), reservation.getExpiresAt(), reservation.getReason(),
                reservation.getCreatedAt());
    }
}
