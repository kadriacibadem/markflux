package com.markflux.product.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void changeStatusReturnsTrueOnlyWhenStatusChanges() {
        Product product = new Product("SKU-1", "Keyboard", null, BigDecimal.TEN, null, ProductStatus.ACTIVE);

        assertThat(product.changeStatus(ProductStatus.ACTIVE)).isFalse();
        assertThat(product.changeStatus(ProductStatus.INACTIVE)).isTrue();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }
}
