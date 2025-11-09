package com.cinehub.promotion.controller;

import com.cinehub.promotion.dto.request.RefundVoucherRequest;
import com.cinehub.promotion.dto.response.RefundVoucherResponse;
import com.cinehub.promotion.service.RefundVoucherService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/promotions/refund-vouchers")
@RequiredArgsConstructor
public class RefundVoucherController {

    private final RefundVoucherService refundVoucherService;

    @PostMapping
    public ResponseEntity<RefundVoucherResponse> createRefundVoucher(@RequestBody RefundVoucherRequest request) {
        RefundVoucherResponse response = refundVoucherService.createRefundVoucher(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RefundVoucherResponse>> getAllRefundVoucher() {
        return ResponseEntity.ok(refundVoucherService.getAllVouchers());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RefundVoucherResponse>> getRefundVoucherByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(refundVoucherService.getVouchersByUser(userId));
    }

    @PutMapping("/use/{code}")
    public ResponseEntity<RefundVoucherResponse> markVoucherAsUsed(@PathVariable String code) {
        return ResponseEntity.ok(refundVoucherService.markAsUsed(code));
    }
}
