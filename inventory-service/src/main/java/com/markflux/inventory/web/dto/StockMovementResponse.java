package com.markflux.inventory.web.dto;

import com.markflux.inventory.domain.MovementType;
import com.markflux.inventory.domain.StockMovement;
import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(UUID id, int quantityChange, MovementType type, String referenceId,
        String reason, Instant createdAt) {

    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(movement.getId(), movement.getQuantityChange(), movement.getType(),
                movement.getReferenceId(), movement.getReason(), movement.getCreatedAt());
    }
}
