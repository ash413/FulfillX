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
public class PaymentEventListener {

    private static final int MAX_ATTEMPTS = 3;

    private final JsonMapper jsonMapper;
    private final InventoryService inventoryService;

    @KafkaListener(topics = "payment.failed")
    public void onPaymentFailed(String message) {
        PaymentFailedEvent event = jsonMapper.readValue(message, PaymentFailedEvent.class);

        for (int attempt = 1; ; attempt++) {
            try {
                inventoryService.release(event.orderId());
                log.info("Released stock for order {} ({})", event.orderId(), event.reason());
                return;
            } catch (OptimisticLockingFailureException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                log.warn("Concurrent update releasing order {}, retrying (attempt {})",
                        event.orderId(), attempt);
            }
        }
    }
}