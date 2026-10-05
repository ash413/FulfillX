package com.fulfillx.orderservice.order;

import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        OrderStatus status,
        List<CreateOrderRequest.Item> items) {
}