package com.cinehub.payment.controller;

import com.cinehub.payment.dto.request.PaymentRequest;
import com.cinehub.payment.dto.response.PaymentResponse;
import com.cinehub.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 🧾 Tạo giao dịch mới (test thủ công hoặc admin)
    @PostMapping
    public PaymentResponse create(@RequestBody PaymentRequest request) {
        return paymentService.createPayment(request);
    }

    // 📄 Lấy toàn bộ giao dịch
    @GetMapping
    public List<PaymentResponse> getAll() {
        return paymentService.getAll();
    }

    // 🔍 Lấy theo ID
    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable UUID id) {
        return paymentService.getById(id);
    }

    // 👤 Lấy theo user
    @GetMapping("/user/{userId}")
    public List<PaymentResponse> getByUser(@PathVariable UUID userId) {
        return paymentService.getByUser(userId);
    }

    // ❌ Xóa giao dịch
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        paymentService.delete(id);
    }
}
