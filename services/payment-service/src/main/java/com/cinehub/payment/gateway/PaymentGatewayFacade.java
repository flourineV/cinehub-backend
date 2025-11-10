package com.cinehub.payment.gateway;

import com.cinehub.payment.entity.PaymentStatus;
import com.cinehub.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentGatewayFacade {

    private final VNPayGateway vnpayGateway;
    private final ZaloPayGateway zaloPayGateway;
    private final PaymentService paymentService;

    public String initiatePayment(UUID bookingId, String provider, BigDecimal amount, String description) {
        return switch (provider.toUpperCase()) {
            case "VNPAY" -> vnpayGateway.createPaymentUrl(bookingId, amount, description);
            case "ZALOPAY" -> zaloPayGateway.createPaymentUrl(bookingId, amount, description);
            default -> throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        };
    }

    public void handleCallback(String provider, Map<String, String> params) {
        PaymentStatus status = switch (provider.toUpperCase()) {
            case "VNPAY" -> vnpayGateway.verifyCallback(params);
            case "ZALOPAY" -> zaloPayGateway.verifyCallback(params);
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };

        UUID bookingId = UUID.fromString(params.get("bookingId"));
        String transactionRef = params.get("txnRef");

        if (status == PaymentStatus.SUCCESS) {
            paymentService.processPaymentSuccess(bookingId, transactionRef, provider);
        } else {
            paymentService.processPaymentFailure(bookingId, transactionRef, "Payment failed");
        }
    }
}
