package com.fulfillx.inventoryservice.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private static final int MAX_ATTEMPTS = 3;

    private final JsonMapper jsonMapper;
    private final InventoryService inventoryService;
    private final InventoryEventPublisher publisher;

    @KafkaListener(topics = "order.created")
    public void onOrderCreated(String message) {
        OrderCreatedEvent event = jsonMapper.readValue(message, OrderCreatedEvent.class);
        log.info("Received OrderCreated: orderId={}, items={}", event.orderId(), event.items());

        try {
            reserveWithRetry(event);
            publisher.publishReserved(new InventoryReservedEvent(event.orderId()));
            log.info("Reserved stock for order {}", event.orderId());
        } catch (StockReservationException e) {
            publisher.publishFailed(new InventoryFailedEvent(event.orderId(), e.getMessage()));
            log.warn("Reservation failed for order {}: {}", event.orderId(), e.getMessage());
        }
    }

    private void reserveWithRetry(OrderCreatedEvent event) {
        for (int attempt = 1; ; attempt++) {
            try {
                inventoryService.reserve(event);
                return;
            } catch (OptimisticLockingFailureException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new StockReservationException("Too much contention, giving up");
                }
                log.warn("Concurrent update on order {}, retrying (attempt {})",
                        event.orderId(), attempt);
            }
        }
    }
}