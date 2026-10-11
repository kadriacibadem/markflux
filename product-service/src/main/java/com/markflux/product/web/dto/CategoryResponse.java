package com.markflux.product.web.dto;

import com.markflux.product.domain.Category;
import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, Instant createdAt) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getCreatedAt());
    }
}
