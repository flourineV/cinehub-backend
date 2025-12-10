package com.cinehub.fnb.events;

import java.math.BigDecimal;
import java.util.UUID;

public record FnbOrderCreatedEvent(
        UUID fnbOrderId,
        UUID userId,
        UUID theaterId,
        BigDecimal totalAmount) {
}
