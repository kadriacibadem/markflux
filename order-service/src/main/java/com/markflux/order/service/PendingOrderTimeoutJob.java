package com.markflux.order.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PendingOrderTimeoutJob {

    private final OrderSagaService sagaService;
    private final Duration pendingTimeout;

    public PendingOrderTimeoutJob(OrderSagaService sagaService,
            @Value("${markflux.order.pending-timeout:2m}") Duration pendingTimeout) {
        this.sagaService = sagaService;
        this.pendingTimeout = pendingTimeout;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void failTimedOutOrders() {
        Instant createdBefore = Instant.now().minus(pendingTimeout);
        int failedCount;
        do {
            failedCount = sagaService.failOrdersCreatedBefore(createdBefore);
        } while (failedCount > 0);
    }
}
