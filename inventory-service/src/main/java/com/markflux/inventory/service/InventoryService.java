package com.markflux.inventory.service;

import com.markflux.common.error.BusinessException;
import com.markflux.common.error.NotFoundException;
import com.markflux.inventory.domain.Inventory;
import com.markflux.inventory.domain.MovementType;
import com.markflux.inventory.domain.StockMovement;
import com.markflux.inventory.repository.InventoryRepository;
import com.markflux.inventory.repository.StockMovementRepository;
import com.markflux.inventory.web.dto.InventoryResponse;
import com.markflux.inventory.web.dto.StockMovementResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository movementRepository;

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID productId) {
        return inventoryRepository.findById(productId)
                .map(InventoryResponse::from)
                .orElseThrow(() -> notFound(productId));
    }

    @Transactional
    public InventoryResponse setStock(UUID productId, int quantity, String reason) {
        Inventory inventory = inventoryRepository.findForUpdate(productId).orElseThrow(() -> notFound(productId));
        int change = inventory.setQuantity(quantity);
        if (change != 0) {
            movementRepository.save(new StockMovement(productId, change, MovementType.CORRECTION, null, reason));
        }
        return InventoryResponse.from(inventory);
    }

    @Transactional
    public InventoryResponse adjustStock(UUID productId, int change, String reason) {
        if (change == 0) {
            throw new BusinessException("INVALID_QUANTITY_CHANGE", "Quantity change cannot be zero");
        }
        Inventory inventory = inventoryRepository.findForUpdate(productId).orElseThrow(() -> notFound(productId));
        inventory.adjust(change);
        movementRepository.save(new StockMovement(productId, change, MovementType.ADJUSTMENT, null, reason));
        return InventoryResponse.from(inventory);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> listMovements(UUID productId, Pageable pageable) {
        if (!inventoryRepository.existsById(productId)) {
            throw notFound(productId);
        }
        return movementRepository.findByProductId(productId, pageable).map(StockMovementResponse::from);
    }

    @Transactional
    public void addProduct(UUID productId) {
        if (!inventoryRepository.existsById(productId)) {
            inventoryRepository.save(new Inventory(productId));
        }
    }

    private static NotFoundException notFound(UUID productId) {
        return new NotFoundException("INVENTORY_NOT_FOUND", "No inventory for product: " + productId);
    }
}
