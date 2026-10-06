package com.fulfillx.orderservice.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final JsonMapper jsonMapper;
    private final OrderService orderService;

    @KafkaListener(topics = "inventory.reserved")
    public void onReserved(String message) {
        InventoryReservedEvent event = jsonMapper.readValue(message, InventoryReservedEvent.class);
        orderService.markStockReserved(event.orderId());
        log.info("Order {} -> STOCK_RESERVED", event.orderId());
    }

    @KafkaListener(topics = "inventory.failed")
    public void onFailed(String message) {
        InventoryFailedEvent event = jsonMapper.readValue(message, InventoryFailedEvent.class);
        orderService.cancel(event.orderId());
        log.warn("Order {} -> CANCELLED ({})", event.orderId(), event.reason());
    }
}