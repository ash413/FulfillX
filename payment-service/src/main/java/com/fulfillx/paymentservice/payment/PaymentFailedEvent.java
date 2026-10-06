package com.fulfillx.paymentservice.payment;

public record PaymentFailedEvent(Long orderId, String reason) {
}