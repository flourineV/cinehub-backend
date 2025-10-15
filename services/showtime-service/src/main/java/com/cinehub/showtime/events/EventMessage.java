// EventMessage.java
package com.cinehub.showtime.events;

import java.time.Instant;

public record EventMessage<T>(
                String eventId,
                String type,
                String version,
                Instant occurredAt,
                T data) {
}
