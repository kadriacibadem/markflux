package com.markflux.product.messaging;

import com.markflux.product.domain.Product;
import com.markflux.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductEvent(UUID productId, String sku, String name, BigDecimal price, UUID categoryId,
        ProductStatus status) {

    public static ProductEvent from(Product product) {
        return new ProductEvent(product.getId(), product.getSku(), product.getName(), product.getPrice(),
                product.getCategoryId(), product.getStatus());
    }
}
