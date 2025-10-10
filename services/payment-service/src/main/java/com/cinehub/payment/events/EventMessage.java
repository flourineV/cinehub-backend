package com.cinehub.payment.events;

import java.time.Instant;

public record EventMessage<T>(
        String eventId,
        String type, // Ví dụ: "BookingCreated", "PaymentCompleted"
        String version,
        Instant occurredAt,
        T data) {
}
