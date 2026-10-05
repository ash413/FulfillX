package com.fulfillx.orderservice.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
    @NotNull Long customerId,
    @NotEmpty List<@Valid Item> items) {

public record Item(
        @NotNull Long productId,
        @Min(1) int quantity) {
}
}