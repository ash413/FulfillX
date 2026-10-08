package com.fulfillx.orderservice.order;

import com.fulfillx.orderservice.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {

        CurrentUser user = CurrentUser.from(jwt);

        if (!user.admin() && !user.id().equals(request.customerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only place orders for your own account"));
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
        }

        String fingerprint = idempotency.fingerprint(request);
        IdempotencyService.Begin begin = idempotency.begin(user.id(), idempotencyKey, fingerprint);

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
                return ResponseEntity.status(422)
                        .body(Map.of("error", "This Idempotency-Key was already used with a different request"));
            }
            case STARTED -> { }
        }

        try {
            OrderResponse created = service.create(request);
            idempotency.complete(user.id(), idempotencyKey, fingerprint, created);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            idempotency.abandon(user.id(), idempotencyKey);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        CurrentUser user = CurrentUser.from(jwt);
        OrderResponse order = service.get(id);

        if (!user.admin() && !user.id().equals(order.customerId())) {
            throw new OrderNotFoundException(id);   // 404, not 403: don't confirm the order exists
        }
        return order;
    }
}