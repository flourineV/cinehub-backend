package com.cinehub.notification.dto.external;

import java.math.BigDecimal;

public record FnbDetail(
                String itemName,
                int quantity,
                BigDecimal unitPrice,
                BigDecimal totalPrice) {
}
