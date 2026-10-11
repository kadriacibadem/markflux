package com.markflux.inventory.service;

import java.util.UUID;

public record StockShortage(UUID productId, int requested, int available) {
}
