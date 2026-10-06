package com.fulfillx.paymentservice.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public void publishCompleted(PaymentCompletedEvent event) {
        kafkaTemplate.send("payment.completed",
                String.valueOf(event.orderId()), jsonMapper.writeValueAsString(event));
    }

    public void publishFailed(PaymentFailedEvent event) {
        kafkaTemplate.send("payment.failed",
                String.valueOf(event.orderId()), jsonMapper.writeValueAsString(event));
    }
}