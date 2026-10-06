package com.fulfillx.inventoryservice.inventory;

public record PaymentFailedEvent(Long orderId, String reason) { }