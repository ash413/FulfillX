package com.fulfillx.inventoryservice.inventory;

public record InventoryFailedEvent(Long orderId, String reason) {
}