package com.cinehub.booking.dto.request;

import java.util.List;
import java.util.UUID;

public record FinalizeBookingRequest(
        List<FnbItemRequest> fnbItems,
        String promotionCode) {
    public record FnbItemRequest(
            UUID fnbId,
            int quantity) {
    }
}