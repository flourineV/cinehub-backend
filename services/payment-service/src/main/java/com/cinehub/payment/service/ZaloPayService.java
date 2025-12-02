package com.cinehub.payment.service;

import com.cinehub.payment.client.ShowtimeServiceClient;
import com.cinehub.payment.config.ZaloPayConfig;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.cinehub.payment.security.AuthChecker;

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
    private final ShowtimeServiceClient showtimeServiceClient;

    @Transactional
    public ZaloPayCreateOrderResponse createOrder(UUID bookingId) throws Exception {
        log.info("💳 Starting ZaloPay order creation for bookingId: {}", bookingId);

        // 1. Kiểm tra Auth (nếu cần test nhanh có thể comment tạm dòng này)
        AuthChecker.requireAuthenticated();

        // 2. Lấy thông tin Transaction
        PaymentTransaction transaction = paymentRepository.findByBookingId(bookingId)
                .stream()
                .filter(t -> t.getStatus() == PaymentStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No PENDING transaction found for booking " + bookingId));

        // 3. Extend seat lock to 10 minutes before creating payment order
        try {
            showtimeServiceClient.extendSeatLockForPayment(
                    transaction.getShowtimeId(),
                    transaction.getSeatIds(),
                    transaction.getUserId(),
                    null // guestSessionId - chưa support guest payment
            );
            log.info("✅ Extended seat lock for payment - bookingId: {}", bookingId);
        } catch (Exception e) {
            log.error("❌ Failed to extend seat lock for bookingId: {}", bookingId, e);
            throw new RuntimeException("Seats are no longer available. Please select seats again.", e);
        }

        // 4. Tạo app_trans_id (QUAN TRỌNG: Phải < 40 ký tự)
        String today = new SimpleDateFormat("yyMMdd").format(new Date());
        // UUID cắt ngắn lấy 8 ký tự đầu để đảm bảo độ dài + tính unique
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        String appTransId = today + "_" + shortId;

        // Update DB
        transaction.setTransactionRef(appTransId);
        transaction.setMethod("ZALOPAY");
        paymentRepository.save(transaction);

        // 4. Chuẩn bị dữ liệu thanh toán
        long appTime = System.currentTimeMillis();
        long amount = transaction.getAmount().longValue();
        String appUser = "CineHub_User";

        // embed_data: chứa redirecturl
        Map<String, String> embedDataMap = new HashMap<>();
        embedDataMap.put("redirecturl", zaloPayConfig.getRedirectUrl());
        String embedData = objectMapper.writeValueAsString(embedDataMap);

        String item = "[]";
        String description = "CineHub - Thanh toan don hang #" + shortId;
        String bankCode = "";

        // 5. Tạo chữ ký MAC (HMAC-SHA256)
        // Format chuẩn: app_id|app_trans_id|app_user|amount|app_time|embed_data|item
        String dataToHash = zaloPayConfig.getAppId() + "|" + appTransId + "|" + appUser + "|" + amount + "|" +
                appTime + "|" + embedData + "|" + item;

        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, zaloPayConfig.getKey1(), dataToHash);

        // 6. Gửi Request dạng FORM-URLENCODED (Fix lỗi -402 và -401)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("app_id", zaloPayConfig.getAppId());
        requestBody.add("app_user", appUser);
        requestBody.add("app_time", String.valueOf(appTime));
        requestBody.add("amount", String.valueOf(amount));
        requestBody.add("app_trans_id", appTransId);
        requestBody.add("embed_data", embedData);
        requestBody.add("item", item);
        requestBody.add("bank_code", bankCode);
        requestBody.add("description", description);
        requestBody.add("callback_url", zaloPayConfig.getCallbackUrl());
        requestBody.add("mac", mac);

        requestBody.add("expire_duration_seconds", "600");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(requestBody, headers);

        String createEndpoint = zaloPayConfig.getEndpoint() + "/create";

        log.info("🚀 Sending Form Request to ZaloPay: {}", requestBody);

        try {
            // Hứng kết quả về dạng Map để linh hoạt
            Map responseMap = restTemplate.postForObject(createEndpoint, entity, Map.class);

            if (responseMap == null) {
                throw new RuntimeException("No response from ZaloPay");
            }

            int returnCode = (int) responseMap.get("return_code");

            if (returnCode != 1) {
                // Lấy thông tin lỗi chi tiết
                String subMsg = (String) responseMap.get("sub_return_message");
                int subCode = (int) responseMap.get("sub_return_code");
                log.error("❌ ZaloPay Error: {} - {}", subCode, subMsg);
                throw new RuntimeException("ZaloPay failed: " + subMsg);
            }

            String orderUrl = (String) responseMap.get("order_url");
            String zpTransToken = (String) responseMap.get("zp_trans_token");

            log.info("✅ ZaloPay Order Created: {}", orderUrl);

            // Map lại vào DTO Response của project bạn
            ZaloPayCreateOrderResponse response = new ZaloPayCreateOrderResponse();
            response.setReturnCode(returnCode);
            response.setOrderUrl(orderUrl);
            response.setSubReturnMessage("Success");
            // Set thêm các field khác nếu DTO của bạn có (ví dụ zp_trans_token)

            return response;

        } catch (Exception e) {
            log.error("🔥 Exception calling ZaloPay: ", e);
            throw new RuntimeException("Failed to initiate payment with ZaloPay: " + e.getMessage());
        }
    }

    public Map<String, Object> checkOrderStatus(String appTransId) throws Exception {
        String appId = zaloPayConfig.getAppId();
        String key1 = zaloPayConfig.getKey1();

        // 1. Tạo MAC cho Query Request
        // Format: appId|appTransId|key1
        String data = appId + "|" + appTransId + "|" + key1;
        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, key1, data);

        // 2. Tạo Form Params
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("app_id", appId);
        params.add("app_trans_id", appTransId);
        params.add("mac", mac);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
        String queryEndpoint = zaloPayConfig.getEndpoint() + "/query";

        log.info("🔍 Checking status for transId: {}", appTransId);

        try {
            Map<String, Object> response = restTemplate.postForObject(queryEndpoint, entity, Map.class);

            if (response != null) {
                log.info("ZaloPay Query Response: {}", response);
                return response;
            } else {
                throw new RuntimeException("Empty response from ZaloPay query");
            }

        } catch (Exception e) {
            log.error("Error querying ZaloPay status", e);
            throw new RuntimeException("Failed to query transaction status");
        }
    }
}