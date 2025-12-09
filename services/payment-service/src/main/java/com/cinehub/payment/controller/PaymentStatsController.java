package com.cinehub.payment.controller;

import com.cinehub.payment.dto.response.PaymentStatsResponse;
import com.cinehub.payment.dto.response.RevenueStatsResponse;
import com.cinehub.payment.security.AuthChecker;
import com.cinehub.payment.service.PaymentStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments/stats")
@RequiredArgsConstructor
public class PaymentStatsController {

    private final PaymentStatsService paymentStatsService;

    @GetMapping("/overview")
    public ResponseEntity<PaymentStatsResponse> getOverview() {
        AuthChecker.requireManagerOrAdmin();
        // Note: Payment stats cannot be filtered by theater (no bookingId/theaterId in
        // payment table)
        // Both Manager and Admin see all payment stats
        return ResponseEntity.ok(paymentStatsService.getOverview());
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<RevenueStatsResponse>> getRevenueStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        AuthChecker.requireManagerOrAdmin();
        // Note: Payment stats cannot be filtered by theater (no bookingId/theaterId in
        // payment table)
        // Both Manager and Admin see all payment stats
        return ResponseEntity.ok(paymentStatsService.getRevenueStats(year, month));
    }
}
