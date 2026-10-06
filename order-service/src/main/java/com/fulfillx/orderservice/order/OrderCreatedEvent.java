package com.fulfillx.orderservice.order;

import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        List<CreateOrderRequest.Item> items) {
}