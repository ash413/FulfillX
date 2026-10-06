package com.fulfillx.orderservice.order;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public void publishOrderCreated(OrderCreatedEvent event) {
        String json = jsonMapper.writeValueAsString(event);
        kafkaTemplate.send("order.created", String.valueOf(event.orderId()), json);
    }
}