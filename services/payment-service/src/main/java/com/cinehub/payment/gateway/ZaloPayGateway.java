package com.cinehub.payment.gateway;

import com.cinehub.payment.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZaloPayGateway implements PaymentGateway {

    @Value("${zalopay.app_id}")
    private String appId;

    @Value("${zalopay.key1}")
    private String key1;

    @Value("${zalopay.endpoint}")
    private String endpoint;

    @Value("${zalopay.callback_url}")
    private String callbackUrl;

    @Value("${zalopay.redirect_url}")
    private String redirectUrl;

    @Override
    public String createPaymentUrl(UUID bookingId, BigDecimal amount, String description) {
        try {
            long appTime = System.currentTimeMillis();
            String appTransId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                    + "_" + appId + "_" + appTime;

            Map<String, Object> order = new LinkedHashMap<>();
            order.put("app_id", appId);
            order.put("app_trans_id", appTransId);
            order.put("app_time", appTime);
            order.put("app_user", "CinehubBooking");
            order.put("amount", amount.intValue());
            order.put("description", description);
            order.put("bank_code", "");
            order.put("item", "[]");
            order.put("embed_data", "{\"bookingId\":\"" + bookingId + "\"}");
            order.put("callback_url", callbackUrl);
            order.put("redirecturl", redirectUrl);

            // MAC (HMAC SHA256)
            String data = appId + "|" + appTransId + "|" + order.get("app_user") + "|" + amount.intValue()
                    + "|" + order.get("app_time") + "|" + description + "|" + key1;
            String mac = hmacSHA256(data, key1);
            order.put("mac", mac);

            // Gửi POST request đến Zalopay endpoint
            String orderUrl = endpoint + "/create";
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(order);
            var client = java.net.http.HttpClient.newHttpClient();
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(orderUrl))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();
            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            Map<?, ?> res = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(response.body(), Map.class);

            log.info("ZaloPay order created: {}", res);

            if (res.get("return_code").toString().equals("1")) {
                return res.get("order_url").toString();
            } else {
                throw new RuntimeException("ZaloPay order creation failed: " + res.get("return_message"));
            }

        } catch (Exception e) {
            log.error("Error creating ZaloPay payment URL", e);
            throw new RuntimeException("ZaloPay payment creation failed");
        }
    }

    @Override
    public PaymentStatus verifyCallback(Map<String, String> callbackParams) {
        try {
            String checksumData = callbackParams.get("data") + key1;
            String expectedMac = hmacSHA256(checksumData, key1);
            if (!expectedMac.equals(callbackParams.get("mac"))) {
                log.warn("ZaloPay callback invalid checksum.");
                return PaymentStatus.FAILED;
            }
            return "1".equals(callbackParams.get("status")) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        } catch (Exception e) {
            log.error("ZaloPay callback verification error", e);
            return PaymentStatus.FAILED;
        }
    }

    private String hmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
