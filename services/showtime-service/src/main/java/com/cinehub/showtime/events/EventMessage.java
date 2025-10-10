// EventMessage.java
package com.cinehub.showtime.events;

import java.time.Instant;

public record EventMessage<T>(
        String eventId,
        String type, // "ShowtimeCreated", "SeatLocked", ...
        String version, // "v1"
        Instant occurredAt,
        T data) {
}
