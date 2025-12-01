package com.cinehub.payment.service;

import com.cinehub.payment.config.ZaloPayConfig;
import com.cinehub.payment.dto.zalopaydto.ZaloPayCreateOrderRequest;
import com.cinehub.payment.dto.zalopaydto.ZaloPayCreateOrderResponse;
import com.cinehub.payment.entity.PaymentTransaction;
import com.cinehub.payment.entity.PaymentStatus;
import com.cinehub.payment.repository.PaymentRepository;
import com.cinehub.payment.utils.HMACUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZaloPayService {

    private final ZaloPayConfig zaloPayConfig;
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public ZaloPayCreateOrderResponse createOrder(UUID bookingId) throws Exception {
        log.info("💳 Starting ZaloPay order creation for bookingId: {}", bookingId);

        PaymentTransaction transaction = paymentRepository.findByBookingId(bookingId)
                .stream()
                .filter(t -> t.getStatus() == PaymentStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No PENDING transaction found for booking " + bookingId));

        String today = new SimpleDateFormat("yyMMdd").format(new Date());
        String appTransId = today + "_" + transaction.getBookingId().toString();

        transaction.setTransactionRef(appTransId);
        transaction.setMethod("ZALOPAY");
        paymentRepository.save(transaction);

        long appTime = System.currentTimeMillis();
        long amount = transaction.getAmount().longValue();
        String appUser = "CineHub_User";

        Map<String, String> embedDataMap = new HashMap<>();
        embedDataMap.put("redirecturl", zaloPayConfig.getRedirectUrl());
        String embedData = objectMapper.writeValueAsString(embedDataMap);

        String item = "[]";
        String description = "CineHub - Payment for booking #" + transaction.getBookingId();
        String bankCode = "";

        // 4. Tạo chữ ký MAC (HMAC-SHA256)
        // Format: app_id|app_trans_id|app_user|amount|app_time|embed_data|item
        String dataToHash = zaloPayConfig.getAppId() + "|" + appTransId + "|" + appUser + "|" + amount + "|" +
                appTime + "|" + embedData + "|" + item;

        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, zaloPayConfig.getKey1(), dataToHash);

        // 5. Build Request DTO (JSON Body)
        ZaloPayCreateOrderRequest requestDto = ZaloPayCreateOrderRequest.builder()
                .appId(Integer.parseInt(zaloPayConfig.getAppId()))
                .appUser(appUser)
                .appTime(appTime)
                .amount(amount)
                .appTransId(appTransId)
                .bankCode(bankCode)
                .embedData(embedData)
                .item(item)
                .callbackUrl(zaloPayConfig.getCallbackUrl())
                .description(description)
                .mac(mac)
                .build();

        // 6. Gọi ZaloPay API với Content-Type: application/json
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ZaloPayCreateOrderRequest> entity = new HttpEntity<>(requestDto, headers);

        String createEndpoint = zaloPayConfig.getEndpoint() + "/create";

        log.info("🚀 Sending JSON request to ZaloPay: {}", requestDto);

        try {
            ZaloPayCreateOrderResponse response = restTemplate.postForObject(createEndpoint, entity,
                    ZaloPayCreateOrderResponse.class);

            if (response == null) {
                throw new RuntimeException("No response from ZaloPay");
            }

            if (response.getReturnCode() != 1) {
                log.error("❌ ZaloPay Error: {} - {}", response.getSubReturnCode(), response.getSubReturnMessage());
                throw new RuntimeException("ZaloPay creation failed: " + response.getSubReturnMessage());
            }

            log.info("✅ ZaloPay Order Created: {}", response.getOrderUrl());
            return response;

        } catch (Exception e) {
            log.error("🔥 Exception calling ZaloPay: ", e);
            throw new RuntimeException("Failed to initiate payment with ZaloPay");
        }
    }
}