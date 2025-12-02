package com.cinehub.payment.controller;

import com.cinehub.payment.config.ZaloPayConfig;
import com.cinehub.payment.dto.request.PaymentCriteria;
import com.cinehub.payment.dto.response.PagedResponse;
import com.cinehub.payment.dto.response.PaymentTransactionResponse;
import com.cinehub.payment.dto.zalopaydto.ZaloPayCreateOrderResponse;
import com.cinehub.payment.dto.zalopaydto.ZaloCallbackDTO;
import com.cinehub.payment.entity.PaymentStatus;
import com.cinehub.payment.security.AuthChecker;
import com.cinehub.payment.service.PaymentService;
import com.cinehub.payment.service.ZaloPayService;
import com.cinehub.payment.utils.HMACUtil;
import com.fasterxml.jackson.databind.JsonNode; // ✅ Import của Jackson
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ZaloPayService zaloPayService;
    private final PaymentService paymentService;
    private final ZaloPayConfig zaloPayConfig;
    private final ObjectMapper objectMapper;

    @PostMapping("/create-zalopay-url")
    public ResponseEntity<?> createZaloPayUrl(@RequestParam UUID bookingId) {
        try {
            ZaloPayCreateOrderResponse response = zaloPayService.createOrder(bookingId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating ZaloPay order", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(@RequestBody ZaloCallbackDTO callbackDTO) {
        Map<String, Object> result = new HashMap<>();

        try {
            String dataStr = callbackDTO.getData();
            String reqMac = callbackDTO.getMac();
            String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, zaloPayConfig.getKey2(), dataStr);

            if (!reqMac.equals(mac)) {
                result.put("return_code", -1);
                result.put("return_message", "mac not equal");
            } else {
                JsonNode dataNode = objectMapper.readTree(dataStr);

                String appTransId = dataNode.get("app_trans_id").asText();
                long amount = dataNode.get("amount").asLong();

                log.info("ZaloPay Callback received for transId: {}", appTransId);

                // 4. Gọi Business Logic
                paymentService.confirmPaymentSuccess(appTransId, "ZaloPay", amount);

                result.put("return_code", 1);
                result.put("return_message", "success");
            }
        } catch (Exception e) {
            log.error("Callback processing error", e);
            result.put("return_code", 0);
            result.put("return_message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/check-status")
    public ResponseEntity<?> checkTransactionStatus(@RequestParam String appTransId) {
        try {
            // 1. Gọi sang ZaloPay check trạng thái thực tế
            Map<String, Object> zpStatus = zaloPayService.checkOrderStatus(appTransId);

            int returnCode = (int) zpStatus.getOrDefault("return_code", -999);
            boolean isSuccess = (returnCode == 1);

            if (isSuccess) {
                long amount = Long.parseLong(zpStatus.get("amount").toString());
                // Gọi hàm confirm (nó có check duplicate nên yên tâm gọi lại)
                paymentService.confirmPaymentSuccess(appTransId, "ZaloPay", amount);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("isSuccess", isSuccess);
            response.put("returnCode", returnCode);
            response.put("returnMessage", zpStatus.get("return_message"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking transaction status", e);
            return ResponseEntity.badRequest().body("Error checking status: " + e.getMessage());
        }
    }

    @GetMapping("/admin/search")
    public ResponseEntity<PagedResponse<PaymentTransactionResponse>> searchPayments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID bookingId,
            @RequestParam(required = false) UUID showtimeId,
            @RequestParam(required = false) String transactionRef,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        AuthChecker.requireAdmin();

        PaymentCriteria criteria = PaymentCriteria.builder()
                .keyword(keyword)
                .userId(userId)
                .bookingId(bookingId)
                .showtimeId(showtimeId)
                .transactionRef(transactionRef)
                .status(status)
                .method(method)
                .fromDate(fromDate)
                .toDate(toDate)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .build();

        PagedResponse<PaymentTransactionResponse> response = paymentService.getPaymentsByCriteria(
                criteria, page, size, sortBy, sortDir);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentTransactionResponse>> getPaymentsByUser(@PathVariable UUID userId) {
        AuthChecker.requireAuthenticated();
        // Verify user can only access their own payments
        UUID currentUserId = UUID.fromString(AuthChecker.getUserIdOrThrow());
        if (!currentUserId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        List<PaymentTransactionResponse> payments = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentTransactionResponse> getPaymentById(@PathVariable UUID id) {
        AuthChecker.requireAuthenticated();
        PaymentTransactionResponse payment = paymentService.getPaymentById(id);

        // Verify user can only access their own payment
        UUID currentUserId = UUID.fromString(AuthChecker.getUserIdOrThrow());
        if (!currentUserId.equals(payment.getUserId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(payment);
    }
}