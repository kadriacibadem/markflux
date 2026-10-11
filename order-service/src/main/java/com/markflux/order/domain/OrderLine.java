package com.markflux.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLine(UUID productId, String productName, BigDecimal unitPrice, int quantity) {

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
