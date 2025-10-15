package com.cinehub.showtime.exception;

/**
 * Exception ném ra khi có xung đột trong quá trình khóa ghế.
 * Ví dụ: Ghế đã bị người dùng khác khóa (SETNX failed).
 */
public class IllegalSeatLockException extends RuntimeException {

    public IllegalSeatLockException(String message) {
        super(message);
    }

    public IllegalSeatLockException(String message, Throwable cause) {
        super(message, cause);
    }
}