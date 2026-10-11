package com.markflux.inventory.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpiredReservationJob {

    private final StockReservationService reservationService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void releaseExpiredReservations() {
        Instant now = Instant.now();
        int releasedCount;
        do {
            releasedCount = reservationService.releaseExpiredReservations(now);
        } while (releasedCount > 0);
    }
}
