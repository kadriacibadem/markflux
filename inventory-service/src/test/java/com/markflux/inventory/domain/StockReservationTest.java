package com.markflux.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StockReservationTest {

    private final UUID productA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final UUID productB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void mergesLinesOfSameProductAndSortsThem() {
        List<ReservationLine> lines = ReservationLine.mergeAndSort(List.of(
                new ReservationLine(productB, 1), new ReservationLine(productA, 2), new ReservationLine(productB, 3)));

        assertThat(lines).containsExactly(new ReservationLine(productA, 2), new ReservationLine(productB, 4));
    }

    @Test
    void reservationCanBeConfirmedThenReleased() {
        StockReservation reservation = StockReservation.reserved(UUID.randomUUID(),
                List.of(new ReservationLine(productA, 1)), Instant.now().plusSeconds(60));

        reservation.confirm();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        reservation.release("Order cancelled");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(reservation.isActive()).isFalse();
    }

    @Test
    void rejectedReservationCannotChange() {
        StockReservation rejected = StockReservation.rejected(UUID.randomUUID(),
                List.of(new ReservationLine(productA, 1)), "Insufficient stock");

        assertThatThrownBy(rejected::confirm).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> rejected.release("x")).isInstanceOf(IllegalStateException.class);
    }
}
