package com.fulfillx.orderservice.order;

public record InventoryFailedEvent(Long orderId, String reason) {
}