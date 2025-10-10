package com.cinehub.payment.service;

import com.cinehub.payment.dto.request.PaymentRequest;
import com.cinehub.payment.dto.response.PaymentResponse;
import com.cinehub.payment.entity.*;
import com.cinehub.payment.repository.PaymentRepository;
import com.cinehub.payment.producer.PaymentProducer;
import com.cinehub.payment.events.BookingCreatedEvent;
import com.cinehub.payment.events.PaymentFailedEvent;
import com.cinehub.payment.events.PaymentSuccessEvent;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProducer paymentProducer;

    /**
     * 🧩 Được gọi khi nhận sự kiện BookingCreatedEvent từ BookingService.
     * Tạo giao dịch thanh toán tương ứng.
     */
    @Transactional
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("💳 Creating PaymentTransaction for bookingId={}, total={}",
                event.bookingId(), event.totalPrice());

        PaymentTransaction transaction = PaymentTransaction.builder()
                .bookingId(event.bookingId())
                .userId(event.userId())
                .amount(event.totalPrice())
                .method("VNPAY") // tạm hardcode, sau này FE truyền lên
                .status(PaymentStatus.PENDING)
                .transactionRef("TXN-" + UUID.randomUUID())
                .build();

        paymentRepository.save(transaction);

        log.info("✅ PaymentTransaction created id={} | ref={}",
                transaction.getId(), transaction.getTransactionRef());

        // 💡 (Giả lập thanh toán thành công)
        completePayment(transaction);
    }

    /**
     * 🔁 Giả lập thanh toán thành công.
     * Trong thực tế: sẽ có callback từ Payment Gateway ở đây.
     */
    @Transactional
    public void completePayment(PaymentTransaction transaction) {
        transaction.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(transaction);

        log.info("💰 Payment SUCCESS for bookingId={} | ref={}",
                transaction.getBookingId(), transaction.getTransactionRef());

        // Gửi event sang NotificationService
        paymentProducer.sendPaymentSuccessEvent(transaction);
    }

    @Transactional
    public void failPayment(PaymentTransaction transaction, String reason) {
        transaction.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(transaction);

        log.warn("❌ Payment FAILED for bookingId={} | reason={}", transaction.getBookingId(), reason);

        paymentProducer.sendPaymentFailedEvent(
                new PaymentFailedEvent(
                        transaction.getId(),
                        transaction.getBookingId(),
                        transaction.getUserId(),
                        transaction.getAmount(),
                        transaction.getMethod(),
                        List.of(),
                        reason));
    }

    // ========== REST CRUD cho FE hoặc Admin test ==========

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .bookingId(request.getBookingId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .method(request.getMethod())
                .status(PaymentStatus.PENDING)
                .transactionRef("TXN-" + UUID.randomUUID())
                .build();

        paymentRepository.save(transaction);
        return mapToResponse(transaction);
    }

    public PaymentResponse getById(UUID id) {
        PaymentTransaction tx = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
        return mapToResponse(tx);
    }

    public List<PaymentResponse> getByUser(UUID userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void successPayment(PaymentTransaction transaction) {
        transaction.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(transaction);

        log.info("💰 Payment SUCCESS for bookingId={} | ref={}",
                transaction.getBookingId(), transaction.getTransactionRef());

        paymentProducer.sendPaymentSuccessEvent(
                new PaymentSuccessEvent(
                        transaction.getId(),
                        transaction.getBookingId(),
                        transaction.getUserId(),
                        transaction.getAmount(),
                        transaction.getMethod(),
                        List.of(), // hoặc seatIds nếu bạn có
                        "Payment success"));
    }

    public List<PaymentResponse> getAll() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID id) {
        paymentRepository.deleteById(id);
    }

    // Helper Mapper
    private PaymentResponse mapToResponse(PaymentTransaction tx) {
        return PaymentResponse.builder()
                .id(tx.getId())
                .bookingId(tx.getBookingId())
                .userId(tx.getUserId())
                .amount(tx.getAmount())
                .method(tx.getMethod())
                .status(tx.getStatus())
                .transactionRef(tx.getTransactionRef())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }
}
