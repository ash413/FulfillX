package com.fulfillx.inventoryservice.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private String name;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Version
    private Long version;

    public void reserve(int quantity) {
        if (quantity > availableQuantity) {
            throw new StockReservationException(
                    "Insufficient stock for product " + productId
                            + ": requested " + quantity + ", available " + availableQuantity);
        }
        availableQuantity -= quantity;
    }
}