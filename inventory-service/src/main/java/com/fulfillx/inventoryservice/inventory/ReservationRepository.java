package com.fulfillx.inventoryservice.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByOrderId(Long orderId);
}