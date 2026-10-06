package com.fulfillx.orderservice.order;

public record PaymentFailedEvent(Long orderId, String reason) { }