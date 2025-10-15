package com.cinehub.booking.exception;

public class SeatAlreadyLockedException extends RuntimeException {
    public SeatAlreadyLockedException(String message) {
        super(message);
    }
}
