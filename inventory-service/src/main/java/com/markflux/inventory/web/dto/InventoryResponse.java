package com.markflux.inventory.web.dto;

import com.markflux.inventory.domain.Inventory;
import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(UUID productId, int quantity, int reservedQuantity, int availableQuantity,
        Instant updatedAt) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(inventory.getProductId(), inventory.getQuantity(),
                inventory.getReservedQuantity(), inventory.availableQuantity(), inventory.getUpdatedAt());
    }
}
