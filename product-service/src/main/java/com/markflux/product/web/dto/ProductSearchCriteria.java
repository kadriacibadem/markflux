package com.markflux.product.web.dto;

import com.markflux.product.domain.ProductStatus;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductSearchCriteria(
        @Size(max = 100) String q,
        UUID categoryId,
        ProductStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        @Size(max = 100) List<UUID> ids) {

    public ProductSearchCriteria onlyActive() {
        return new ProductSearchCriteria(q, categoryId, ProductStatus.ACTIVE, minPrice, maxPrice, ids);
    }
}
