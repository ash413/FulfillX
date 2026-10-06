package com.fulfillx.orderservice.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxEventRepository repository;
    private final JsonMapper jsonMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String aggregateType, Object aggregateId, String topic, Object event) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setAggregateId(String.valueOf(aggregateId));
        outboxEvent.setTopic(topic);
        outboxEvent.setPayload(jsonMapper.writeValueAsString(event));
        repository.save(outboxEvent);
    }
}