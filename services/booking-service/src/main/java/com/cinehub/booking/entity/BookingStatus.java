package com.cinehub.booking.entity;

public enum BookingStatus {
    PENDING,
    AWAITING_PAYMENT,
    CONFIRMED,
    CANCELLED,      // User cancel trước khi thanh toán hoặc admin cancel
    REFUNDED,       // User cancel sau khi thanh toán (có voucher hoàn)
    EXPIRED,
}
