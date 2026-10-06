package com.fulfillx.inventoryservice.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class IdempotencyGuard {

    private final JdbcTemplate jdbc;

    /** Returns true the first time this (handler, key) is seen, false for any repeat. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean firstTime(String handler, String eventKey) {
        int rows = jdbc.update(
                "INSERT INTO processed_events (handler, event_key) VALUES (?, ?) ON CONFLICT DO NOTHING",
                handler, eventKey);
        return rows == 1;
    }
}