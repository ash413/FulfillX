package com.fulfillx.paymentservice.payment;

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
    private final PaymentService paymentService;

    @KafkaListener(topics = "inventory.reserved")
    public void onInventoryReserved(String message) {
        InventoryReservedEvent event = jsonMapper.readValue(message, InventoryReservedEvent.class);

        paymentService.process(event.orderId()).ifPresentOrElse(
                payment -> log.info("Payment {} for order {}", payment.getStatus(), event.orderId()),
                () -> log.info("Order {} already processed, skipping duplicate", event.orderId()));
    }
}