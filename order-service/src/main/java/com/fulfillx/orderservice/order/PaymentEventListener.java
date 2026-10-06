package com.fulfillx.orderservice.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final JsonMapper jsonMapper;
    private final OrderService orderService;

    @KafkaListener(topics = "payment.completed")
    public void onCompleted(String message) {
        PaymentCompletedEvent event = jsonMapper.readValue(message, PaymentCompletedEvent.class);
        orderService.confirm(event.orderId());
        log.info("Order {} -> CONFIRMED", event.orderId());
    }

    @KafkaListener(topics = "payment.failed")
    public void onFailed(String message) {
        PaymentFailedEvent event = jsonMapper.readValue(message, PaymentFailedEvent.class);
        orderService.cancel(event.orderId());
        log.warn("Order {} -> CANCELLED (payment failed: {})", event.orderId(), event.reason());
    }
}