package com.cinehub.booking.config;

public class SeatAlreadyLockedException extends RuntimeException {
    public SeatAlreadyLockedException(String seatId) {
        super("Seat already locked: " + seatId);
    }
}
