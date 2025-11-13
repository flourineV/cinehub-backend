package com.cinehub.payment.controller;

import com.cinehub.payment.service.PaymentService;
import com.cinehub.payment.exception.PaymentProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/callback/success")
    public ResponseEntity<String> mockSuccessCallback(
            @RequestParam UUID bookingId,
            @RequestParam String transactionRef) {

        String method = "VISA_MOCK";

        try {
            log.info("Receiving mock success callback for bookingId: {}", bookingId);
            paymentService.processPaymentSuccess(bookingId, transactionRef, method);

            return ResponseEntity.ok("Payment confirmed and events sent.");

        } catch (PaymentProcessingException e) {

            log.warn("Callback failed due to business logic: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Xử lý lỗi hệ thống bất ngờ (Lỗi DB, lỗi mạng, etc.)
            log.error("Unexpected error during success callback for bookingId {}: {}", bookingId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Internal Server Error.");
        }
    }

    @PostMapping("/callback/failed")
    public ResponseEntity<String> mockFailedCallback(
            @RequestParam UUID bookingId,
            @RequestParam String transactionRef,
            @RequestParam String reason) {

        try {
            log.warn("📢 Receiving mock failed callback for bookingId: {} | Reason: {}", bookingId, reason);
            paymentService.processPaymentFailure(bookingId, transactionRef, reason);

            // Trả về HTTP 200 OK cho WebHook/Callback
            return ResponseEntity.ok("Payment failed and events sent.");

        } catch (PaymentProcessingException e) {
            // Xử lý lỗi nghiệp vụ
            log.warn("❌ Callback failed due to business logic: {}", e.getMessage());
            throw e; // Ném lại để GlobalExceptionHandler xử lý
        } catch (Exception e) {
            // Xử lý lỗi hệ thống bất ngờ
            log.error("❌ Unexpected error during failed callback for bookingId {}: {}", bookingId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Internal Server Error.");
        }
    }
}