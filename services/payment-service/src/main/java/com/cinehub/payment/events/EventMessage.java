package com.cinehub.payment.events;

import java.time.Instant;

public record EventMessage<T>(
                String eventId,
                String type,
                String version,
                Instant occurredAt,
                T data) {
}
