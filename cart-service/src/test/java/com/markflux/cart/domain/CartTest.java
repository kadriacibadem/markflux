package com.markflux.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.markflux.common.error.BusinessException;
import com.markflux.common.error.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartTest {

    private final Cart cart = new Cart(UUID.randomUUID());
    private final UUID keyboard = UUID.randomUUID();
    private final UUID mouse = UUID.randomUUID();

    @Test
    void addingSameProductIncreasesQuantity() {
        cart.addItem(keyboard, "Keyboard", new BigDecimal("100.00"), 1);
        cart.addItem(keyboard, "Keyboard", new BigDecimal("100.00"), 2);
        cart.addItem(mouse, "Mouse", new BigDecimal("40.50"), 2);

        assertThat(cart.getItems()).hasSize(2);
        assertThat(cart.totalQuantity()).isEqualTo(5);
        assertThat(cart.totalAmount()).isEqualByComparingTo("381.00");
    }

    @Test
    void quantityMustStayWithinLimits() {
        cart.addItem(keyboard, "Keyboard", BigDecimal.TEN, Cart.MAX_QUANTITY);

        assertThatThrownBy(() -> cart.addItem(keyboard, "Keyboard", BigDecimal.TEN, 1))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> cart.changeQuantity(keyboard, 0)).isInstanceOf(BusinessException.class);
    }

    @Test
    void removesOnlyOrderedProducts() {
        cart.addItem(keyboard, "Keyboard", BigDecimal.TEN, 1);
        cart.addItem(mouse, "Mouse", BigDecimal.TEN, 1);

        cart.removeProducts(List.of(keyboard));

        assertThat(cart.getItems()).extracting(CartItem::getProductId).containsExactly(mouse);
    }
}
