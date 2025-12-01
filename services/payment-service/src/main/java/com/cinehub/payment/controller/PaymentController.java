package com.cinehub.payment.controller;

import com.cinehub.payment.config.ZaloPayConfig;
import com.cinehub.payment.dto.zalopaydto.ZaloPayCreateOrderResponse;
import com.cinehub.payment.dto.zalopaydto.ZaloCallbackDTO;
import com.cinehub.payment.service.PaymentService;
import com.cinehub.payment.service.ZaloPayService;
import com.cinehub.payment.utils.HMACUtil;
import com.fasterxml.jackson.databind.JsonNode; // ✅ Import của Jackson
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payment")
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
            // 1. Lấy data và mac từ DTO (Spring tự map JSON vào DTO này rồi)
            String dataStr = callbackDTO.getData();
            String reqMac = callbackDTO.getMac();

            // 2. Kiểm tra chữ ký (Security Check)
            // mac = HMAC(key2, data)
            String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, zaloPayConfig.getKey2(), dataStr);

            if (!reqMac.equals(mac)) {
                // Chữ ký không khớp -> Giả mạo
                result.put("return_code", -1);
                result.put("return_message", "mac not equal");
            } else {
                // 3. Chữ ký hợp lệ -> Parse dữ liệu dataStr bằng Jackson
                // dataStr ví dụ: {"app_trans_id": "...", "amount": 50000, ...}

                JsonNode dataNode = objectMapper.readTree(dataStr); // ✅ Dùng Jackson thay JSONObject

                String appTransId = dataNode.get("app_trans_id").asText();
                long amount = dataNode.get("amount").asLong();

                log.info("🔔 ZaloPay Callback received for transId: {}", appTransId);

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
}