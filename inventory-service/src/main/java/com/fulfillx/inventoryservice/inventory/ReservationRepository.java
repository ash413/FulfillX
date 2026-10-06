package com.fulfillx.inventoryservice.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByOrderId(Long orderId);

    List<Reservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);
}