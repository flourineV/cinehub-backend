package com.cinehub.payment.controller;

import com.cinehub.payment.dto.request.RefundVoucherRequest;
import com.cinehub.payment.dto.response.RefundVoucherResponse;
import com.cinehub.payment.service.RefundVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/refund-vouchers")
@RequiredArgsConstructor
public class RefundVoucherController {

    private final RefundVoucherService refundVoucherService;

    // =====================================================
    // 🧾 Tạo voucher hoàn tiền
    // =====================================================
    @PostMapping
    public ResponseEntity<RefundVoucherResponse> create(@RequestBody RefundVoucherRequest request) {
        RefundVoucherResponse response = refundVoucherService.createRefundVoucher(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =====================================================
    // 🔍 Lấy tất cả voucher (admin)
    // =====================================================
    @GetMapping
    public ResponseEntity<List<RefundVoucherResponse>> getAll() {
        return ResponseEntity.ok(refundVoucherService.getAllVouchers());
    }

    // =====================================================
    // 👤 Lấy voucher theo user
    // =====================================================
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RefundVoucherResponse>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(refundVoucherService.getVouchersByUser(userId));
    }

    // =====================================================
    // ✅ Đánh dấu voucher đã dùng (khi user thanh toán)
    // =====================================================
    @PutMapping("/use/{code}")
    public ResponseEntity<RefundVoucherResponse> markAsUsed(@PathVariable String code) {
        return ResponseEntity.ok(refundVoucherService.markAsUsed(code));
    }
}
