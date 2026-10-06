package com.fulfillx.inventoryservice.inventory;

public class StockReservationException extends RuntimeException {
    public StockReservationException(String message) {
        super(message);
    }
}