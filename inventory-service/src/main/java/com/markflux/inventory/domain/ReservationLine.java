package com.markflux.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Embeddable
public record ReservationLine(@Column(name = "product_id") UUID productId, int quantity) {

    public static List<ReservationLine> mergeAndSort(List<ReservationLine> lines) {
        Map<UUID, Integer> quantities = new TreeMap<>();
        for (ReservationLine line : lines) {
            quantities.merge(line.productId(), line.quantity(), Integer::sum);
        }
        return quantities.entrySet().stream()
                .map(entry -> new ReservationLine(entry.getKey(), entry.getValue()))
                .toList();
    }
}
