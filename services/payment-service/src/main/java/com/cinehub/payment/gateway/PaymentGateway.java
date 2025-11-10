package com.cinehub.payment.gateway;

import com.cinehub.payment.entity.PaymentStatus;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface PaymentGateway {
    String createPaymentUrl(UUID bookingId, BigDecimal amount, String description);

    PaymentStatus verifyCallback(Map<String, String> callbackParams);
}
