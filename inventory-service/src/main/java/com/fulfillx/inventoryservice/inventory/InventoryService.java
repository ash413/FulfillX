package com.fulfillx.inventoryservice.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository items;
    private final ReservationRepository reservations;
    private final IdempotencyGuard guard;

    @Transactional
    public void reserve(OrderCreatedEvent event) {
        if (!guard.firstTime("inventory.reserve", String.valueOf(event.orderId()))) {
            log.info("Order {} already processed, skipping duplicate", event.orderId());
            return;
        }

        for (OrderCreatedEvent.Item line : event.items()) {
            InventoryItem item = items.findById(line.productId())
                    .orElseThrow(() -> new StockReservationException(
                            "Unknown product " + line.productId()));

            item.reserve(line.quantity());

            Reservation reservation = new Reservation();
            reservation.setOrderId(event.orderId());
            reservation.setProductId(line.productId());
            reservation.setQuantity(line.quantity());
            reservation.setStatus(ReservationStatus.RESERVED);
            reservations.save(reservation);
        }
    }

    @Transactional
    public void release(Long orderId) {
        if (!guard.firstTime("inventory.release", String.valueOf(orderId))) {
            log.info("Release for order {} already processed, skipping duplicate", orderId);
            return;
        }

        List<Reservation> held =
                reservations.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        for (Reservation r : held) {
            InventoryItem item = items.findById(r.getProductId())
                    .orElseThrow(() -> new StockReservationException(
                            "Unknown product " + r.getProductId()));
            item.release(r.getQuantity());
            r.setStatus(ReservationStatus.RELEASED);
        }
    }
}