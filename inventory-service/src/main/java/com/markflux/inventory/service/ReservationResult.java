package com.markflux.inventory.service;

import com.markflux.inventory.domain.StockReservation;
import java.util.List;

public sealed interface ReservationResult {

    record Reserved(StockReservation reservation) implements ReservationResult {
    }

    record Rejected(List<StockShortage> shortages) implements ReservationResult {
    }

    record AlreadyExists(StockReservation reservation) implements ReservationResult {
    }
}
