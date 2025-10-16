package com.cinehub.payment.service;

import com.cinehub.payment.entity.PaymentTransaction;
import com.cinehub.payment.entity.PaymentStatus;
import com.cinehub.payment.events.BookingCreatedEvent;
import com.cinehub.payment.events.BookingFinalizedEvent;
import com.cinehub.payment.events.PaymentSuccessEvent;
import com.cinehub.payment.events.PaymentFailedEvent;
import com.cinehub.payment.producer.PaymentProducer;
import com.cinehub.payment.repository.PaymentRepository;
import com.cinehub.payment.exception.PaymentProcessingException; // ✅ Đảm bảo import này
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Optional; // Cần thiết cho logic tìm kiếm chính xác
import java.util.List; // Thêm import List

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

        private final PaymentProducer paymentProducer;
        private final PaymentRepository paymentRepository;

        // --- Hàm khởi tạo PENDING (Giữ nguyên) ---
        @Transactional
        public void createPendingTransaction(BookingCreatedEvent event) {
                PaymentTransaction pendingTxn = PaymentTransaction.builder()
                                .bookingId(event.bookingId())
                                .userId(event.userId())
                                .amount(event.totalPrice())
                                .method("INIT_GATEWAY")
                                .status(PaymentStatus.PENDING)
                                .transactionRef("TXN_PENDING_" + UUID.randomUUID().toString())
                                .build();

                paymentRepository.save(pendingTxn);
                log.info("💾 PENDING Transaction created for bookingId: {}", event.bookingId());
        }

        // --- Hàm Xử lý Thành công ---
        @Transactional
        public void processPaymentSuccess(UUID bookingId, String transactionRef, String paymentMethod) {

                Optional<PaymentTransaction> optionalTxn = paymentRepository.findByBookingId(bookingId)
                                .stream() // ✅ CHUYỂN LIST SANG STREAM ĐỂ SỬ DỤNG FILTER
                                .filter(t -> t.getStatus() == PaymentStatus.PENDING)
                                .findFirst(); // ✅ LẤY PHẦN TỬ ĐẦU TIÊN (HOẶC OPTIONAL RỖNG)

                if (optionalTxn.isEmpty()) {
                        log.error("⚠️ Transaction not found or not PENDING for bookingId {}. Cannot confirm payment.",
                                        bookingId);
                        // ✅ SỬ DỤNG CUSTOM EXCEPTION
                        throw new PaymentProcessingException(
                                        "Transaction not found or not PENDING for bookingId: " + bookingId);
                }

                PaymentTransaction txn = optionalTxn.get();

                // Kiểm tra Idempotency (redundant nếu filter PENDING, nhưng là safety check
                // tốt)
                if (txn.getStatus() == PaymentStatus.SUCCESS) {
                        log.warn("Transaction for bookingId {} already SUCCESS. Skipping.", bookingId);
                        return;
                }

                // 2. Cập nhật thông tin giao dịch
                txn.setStatus(PaymentStatus.SUCCESS);
                txn.setTransactionRef(transactionRef);
                txn.setMethod(paymentMethod);
                paymentRepository.save(txn);
                log.info("✅ SUCCESS: Payment transaction updated for bookingId: {}", bookingId);

                // 3. Gửi Event phản hồi
                PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                                txn.getId(),
                                txn.getBookingId(),
                                txn.getUserId(),
                                txn.getAmount(),
                                txn.getMethod(),
                                null, // Seat IDs
                                "PAYMENT_SUCCESS");

                paymentProducer.sendPaymentSuccessEvent(successEvent);
        }

        // --- Hàm Xử lý Thất bại ---
        @Transactional
        public void processPaymentFailure(UUID bookingId, String transactionRef, String reason) {

                Optional<PaymentTransaction> optionalTxn = paymentRepository.findByBookingId(bookingId)
                                .stream() // ✅ CHUYỂN LIST SANG STREAM ĐỂ SỬ DỤNG FILTER
                                .filter(t -> t.getStatus() == PaymentStatus.PENDING)
                                .findFirst();

                if (optionalTxn.isEmpty()) {
                        log.error("⚠️ Transaction not found or not PENDING for bookingId {}. Cannot record failure.",
                                        bookingId);
                        // ✅ SỬ DỤNG CUSTOM EXCEPTION
                        throw new PaymentProcessingException(
                                        "Transaction not found or not PENDING for bookingId: " + bookingId);
                }

                PaymentTransaction txn = optionalTxn.get();

                // 2. Cập nhật trạng thái
                txn.setStatus(PaymentStatus.FAILED);
                txn.setTransactionRef(transactionRef);
                paymentRepository.save(txn);
                log.warn("❌ FAILED: Payment transaction updated for bookingId: {}", bookingId);

                // 3. Gửi Event phản hồi
                PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                                txn.getId(),
                                txn.getBookingId(),
                                txn.getUserId(),
                                txn.getAmount(),
                                txn.getMethod(),
                                null, // Seat IDs
                                reason);

                paymentProducer.sendPaymentFailedEvent(failedEvent);
        }

        @Transactional
        public void updateFinalAmount(BookingFinalizedEvent event) {
                log.info("💰 Updating Payment amount after finalization | bookingId={} | newAmount={}",
                                event.bookingId(), event.finalPrice());

                // Tìm transaction đang PENDING cho booking này
                Optional<PaymentTransaction> optionalTxn = paymentRepository.findByBookingId(event.bookingId())
                                .stream()
                                .filter(t -> t.getStatus() == PaymentStatus.PENDING)
                                .findFirst();

                if (optionalTxn.isEmpty()) {
                        log.warn("⚠️ No PENDING transaction found for bookingId {}. Skipping update.",
                                        event.bookingId());
                        return;
                }

                PaymentTransaction txn = optionalTxn.get();
                txn.setAmount(event.finalPrice());
                paymentRepository.save(txn);

                log.info("✅ Updated transaction amount for bookingId {} → {}", event.bookingId(), event.finalPrice());
        }

}