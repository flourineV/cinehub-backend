package com.cinehub.payment.gateway;

import com.cinehub.payment.entity.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class VNPayGateway implements PaymentGateway {

    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    @Value("${vnpay.payUrl}")
    private String payUrl;

    @Override
    public String createPaymentUrl(UUID bookingId, BigDecimal amount, String description) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", tmnCode);
            params.put("vnp_Amount", String.valueOf(amount.multiply(BigDecimal.valueOf(100)).intValue())); // nhân 100
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", bookingId.toString());
            params.put("vnp_OrderInfo", description);
            params.put("vnp_OrderType", "other");
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", returnUrl);
            params.put("vnp_IpAddr", "127.0.0.1");
            params.put("vnp_CreateDate", DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));

            // ✅ Build query string và tạo checksum
            List<String> fieldNames = new ArrayList<>(params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
                String fieldName = itr.next();
                String fieldValue = params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            String secureHash = hmacSHA512(hashSecret, hashData.toString());
            query.append("&vnp_SecureHash=").append(secureHash);
            String paymentUrl = payUrl + "?" + query;

            log.info("VNPay URL generated: {}", paymentUrl);
            return paymentUrl;
        } catch (Exception e) {
            log.error("Error creating VNPay URL: {}", e.getMessage());
            throw new RuntimeException("Cannot create VNPay URL");
        }
    }

    @Override
    public PaymentStatus verifyCallback(Map<String, String> callbackParams) {
        try {
            String vnp_SecureHash = callbackParams.remove("vnp_SecureHash");
            List<String> fieldNames = new ArrayList<>(callbackParams.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
                String fieldName = itr.next();
                String fieldValue = callbackParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=').append(fieldValue);
                    if (itr.hasNext())
                        hashData.append('&');
                }
            }

            String calculatedHash = hmacSHA512(hashSecret, hashData.toString());
            boolean valid = calculatedHash.equals(vnp_SecureHash);

            String responseCode = callbackParams.get("vnp_ResponseCode");
            log.info("VNPay callback: responseCode={} valid={}", responseCode, valid);

            if (valid && "00".equals(responseCode)) {
                return PaymentStatus.SUCCESS;
            } else {
                return PaymentStatus.FAILED;
            }
        } catch (Exception e) {
            log.error("Error verifying VNPay callback: {}", e.getMessage());
            return PaymentStatus.FAILED;
        }
    }

    private String hmacSHA512(String key, String data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] hashBytes = md.digest((key + data).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
