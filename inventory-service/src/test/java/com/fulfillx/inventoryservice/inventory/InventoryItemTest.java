package com.fulfillx.inventoryservice.inventory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemTest {

    private InventoryItem item(int quantity) {
        InventoryItem item = new InventoryItem();
        item.setProductId(103L);
        item.setName("PS5 Console");
        item.setAvailableQuantity(quantity);
        return item;
    }

    @Test
    void reserveReducesStock() {
        InventoryItem item = item(5);
        item.reserve(2);
        assertThat(item.getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    void reserveMoreThanAvailableFailsAndLeavesStockUntouched() {
        InventoryItem item = item(1);

        assertThatThrownBy(() -> item.reserve(2))
                .isInstanceOf(StockReservationException.class)
                .hasMessageContaining("Insufficient stock");
        assertThat(item.getAvailableQuantity()).isEqualTo(1);
    }

    @Test
    void releaseRestoresStock() {
        InventoryItem item = item(0);
        item.release(3);
        assertThat(item.getAvailableQuantity()).isEqualTo(3);
    }
}