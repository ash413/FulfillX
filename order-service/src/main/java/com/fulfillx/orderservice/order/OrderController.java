package com.fulfillx.orderservice.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;
    private final IdempotencyService idempotency;

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
        }

        String fingerprint = idempotency.fingerprint(request);
        IdempotencyService.Begin begin = idempotency.begin(idempotencyKey, fingerprint);

        switch (begin.outcome()) {
            case REPLAY -> {
                return ResponseEntity.ok()
                        .header("Idempotent-Replayed", "true")
                        .body(begin.replay());
            }
            case IN_PROGRESS -> {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "A request with this Idempotency-Key is still in progress"));
            }
            case MISMATCH -> {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("error", "This Idempotency-Key was already used with a different request"));
            }
            case STARTED -> { }
        }

        try {
            OrderResponse created = service.create(request);
            idempotency.complete(idempotencyKey, fingerprint, created);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            idempotency.abandon(idempotencyKey);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return service.get(id);
    }
}