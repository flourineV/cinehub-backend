package com.cinehub.payment.controller;

import com.cinehub.payment.gateway.ZaloPayGateway;
import com.cinehub.payment.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment/zalopay")
public class ZaloPayController {

    private final ZaloPayGateway zaloPayGateway;

    @PostMapping("/callback")
    public String callback(@RequestParam Map<String, String> params) {
        PaymentStatus status = zaloPayGateway.verifyCallback(params);
        log.info("ZaloPay callback result: {}", status);
        return "{\"return_code\":1,\"return_message\":\"OK\"}";
    }
}
