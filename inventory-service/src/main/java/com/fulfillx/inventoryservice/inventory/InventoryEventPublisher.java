package com.fulfillx.inventoryservice.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public void publishReserved(InventoryReservedEvent event) {
        kafkaTemplate.send("inventory.reserved",
                String.valueOf(event.orderId()), jsonMapper.writeValueAsString(event));
    }

    public void publishFailed(InventoryFailedEvent event) {
        kafkaTemplate.send("inventory.failed",
                String.valueOf(event.orderId()), jsonMapper.writeValueAsString(event));
    }
}