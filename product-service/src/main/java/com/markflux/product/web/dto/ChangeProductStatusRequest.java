package com.markflux.product.web.dto;

import com.markflux.product.domain.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeProductStatusRequest(@NotNull ProductStatus status) {
}
