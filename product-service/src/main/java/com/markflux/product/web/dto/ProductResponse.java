package com.markflux.product.web.dto;

import com.markflux.product.domain.Product;
import com.markflux.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(UUID id, String sku, String name, String description, BigDecimal price,
        UUID categoryId, ProductStatus status, Instant createdAt, Instant updatedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getDescription(),
                product.getPrice(), product.getCategoryId(), product.getStatus(), product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
