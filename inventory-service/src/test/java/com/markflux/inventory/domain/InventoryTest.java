package com.markflux.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.markflux.common.error.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryTest {

    private final Inventory inventory = new Inventory(UUID.randomUUID());

    @Test
    void reservingLowersAvailableButNotOnHandQuantity() {
        inventory.adjust(10);

        inventory.reserve(4);

        assertThat(inventory.getQuantity()).isEqualTo(10);
        assertThat(inventory.availableQuantity()).isEqualTo(6);
    }

    @Test
    void cannotReserveMoreThanAvailable() {
        inventory.adjust(3);
        inventory.reserve(2);

        assertThat(inventory.canReserve(2)).isFalse();
        assertThatThrownBy(() -> inventory.reserve(2)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void commitRemovesUnitsAndRestockAddsThemBack() {
        inventory.adjust(10);
        inventory.reserve(4);

        inventory.commitReservation(4);
        assertThat(inventory.getQuantity()).isEqualTo(6);
        assertThat(inventory.getReservedQuantity()).isZero();

        inventory.restock(4);
        assertThat(inventory.getQuantity()).isEqualTo(10);
    }

    @Test
    void stockCannotBeLowerThanReserved() {
        inventory.adjust(10);
        inventory.reserve(8);

        assertThatThrownBy(() -> inventory.setQuantity(5)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> inventory.adjust(-3)).isInstanceOf(BusinessException.class);
        assertThat(inventory.setQuantity(8)).isEqualTo(-2);
    }
}
