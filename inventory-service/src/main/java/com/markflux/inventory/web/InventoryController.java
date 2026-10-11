package com.markflux.inventory.web;

import com.markflux.common.web.PageResponse;
import com.markflux.inventory.service.InventoryService;
import com.markflux.inventory.web.dto.AdjustStockRequest;
import com.markflux.inventory.web.dto.InventoryResponse;
import com.markflux.inventory.web.dto.SetStockRequest;
import com.markflux.inventory.web.dto.StockMovementResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public InventoryResponse getInventory(@PathVariable UUID productId) {
        return inventoryService.getInventory(productId);
    }

    @PutMapping("/{productId}")
    public InventoryResponse setStock(@PathVariable UUID productId, @Valid @RequestBody SetStockRequest request) {
        return inventoryService.setStock(productId, request.quantity(), request.reason());
    }

    @PostMapping("/{productId}/adjustments")
    public InventoryResponse adjustStock(@PathVariable UUID productId,
            @Valid @RequestBody AdjustStockRequest request) {
        return inventoryService.adjustStock(productId, request.quantityChange(), request.reason());
    }

    @GetMapping("/{productId}/movements")
    public PageResponse<StockMovementResponse> listMovements(@PathVariable UUID productId,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.from(inventoryService.listMovements(productId, pageable));
    }
}
