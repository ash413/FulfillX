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

    @Transactional
    public void reserve(OrderCreatedEvent event) {
        if (reservations.existsByOrderId(event.orderId())) {
            log.info("Order {} already reserved, skipping duplicate", event.orderId());
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