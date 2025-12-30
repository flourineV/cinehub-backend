package com.cinehub.payment.service;

import com.cinehub.payment.dto.request.PaymentCriteria;
import com.cinehub.payment.dto.response.PagedResponse;
import com.cinehub.payment.dto.response.PaymentTransactionResponse;
import com.cinehub.payment.entity.PaymentTransaction;
import com.cinehub.payment.entity.PaymentStatus;
import com.cinehub.payment.events.BookingCreatedEvent;
import com.cinehub.payment.events.BookingFinalizedEvent;
import com.cinehub.payment.events.FnbOrderCreatedEvent;
import com.cinehub.payment.events.PaymentBookingSuccessEvent;
import com.cinehub.payment.events.PaymentFnbSuccessEvent;
import com.cinehub.payment.events.SeatUnlockedEvent;
import com.cinehub.payment.events.PaymentBookingFailedEvent;
import com.cinehub.payment.events.PaymentFnbFailedEvent;
import com.cinehub.payment.producer.PaymentProducer;
import com.cinehub.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

        private final PaymentProducer paymentProducer;
        private final PaymentRepository paymentRepository;
        private final com.cinehub.payment.adapter.client.UserProfileClient userProfileClient;

        // 1. Tạo Transaction khi Booking vừa tạo (Giữ nguyên)
        @Transactional
        public void createPendingTransaction(BookingCreatedEvent event) {
                // Kiểm tra xem đã tồn tại chưa để tránh duplicate
                if (paymentRepository.existsByBookingId(event.bookingId())) {
                        log.warn("Transaction already exists for bookingId: {}. Skipping.", event.bookingId());
                        return;
                }

                PaymentTransaction pendingTxn = PaymentTransaction.builder()
                                .bookingId(event.bookingId())
                                .userId(event.userId())
                                .showtimeId(event.showtimeId())
                                .seatIds(event.seatIds())
                                .amount(event.totalPrice())
                                .method("UNKNOWN") // Chưa biết user chọn ví nào, lát nữa update sau
                                .status(PaymentStatus.PENDING)
                                .transactionRef("TXN_INIT_" + UUID.randomUUID()) // Tạm thời
                                .build();

                paymentRepository.save(pendingTxn);
                log.info("PENDING Transaction created for bookingId: {}", event.bookingId());
        }

        // Tạo Transaction khi FnbOrder vừa tạo
        @Transactional
        public void createPendingTransactionForFnb(FnbOrderCreatedEvent event) {
                // Kiểm tra xem đã tồn tại chưa để tránh duplicate
                if (paymentRepository.existsByFnbOrderId(event.fnbOrderId())) {
                        log.warn("Transaction already exists for fnbOrderId: {}. Skipping.", event.fnbOrderId());
                        return;
                }

                PaymentTransaction pendingTxn = PaymentTransaction.builder()
                                .fnbOrderId(event.fnbOrderId())
                                .userId(event.userId())
                                .showtimeId(null) // FnB standalone không có showtime
                                .seatIds(new ArrayList<>()) // Không có ghế
                                .amount(event.totalAmount())
                                .method("UNKNOWN")
                                .status(PaymentStatus.PENDING)
                                .transactionRef("TXN_FNB_" + UUID.randomUUID())
                                .build();

                paymentRepository.save(pendingTxn);
                log.info("PENDING Transaction created for fnbOrderId: {} | amount={}",
                                event.fnbOrderId(), event.totalAmount());
        }

        @Transactional
        public void confirmPaymentSuccess(String appTransId, String merchantTransId, long amountPaid) {

                PaymentTransaction txn = paymentRepository.findByTransactionRef(appTransId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Transaction not found for ref: " + appTransId));

                if (txn.getStatus() == PaymentStatus.SUCCESS) {
                        log.warn("⚠️ Transaction {} already SUCCESS. Ignoring callback.", appTransId);
                        return;
                }

                if (txn.getAmount().longValue() != amountPaid) {
                        log.error("Amount mismatch! Expected: {}, Paid: {}", txn.getAmount(), amountPaid);
                        return;
                }

                // Update DB
                txn.setStatus(PaymentStatus.SUCCESS);
                txn.setMethod("ZALOPAY"); // Hoặc lấy từ callback
                paymentRepository.save(txn);

                log.info("💰 Payment SUCCESS for bookingId: {} | fnbOrderId: {}",
                                txn.getBookingId(), txn.getFnbOrderId());

                // Bắn Event báo cho Booking Service (chỉ khi có bookingId)
                if (txn.getBookingId() != null) {
                        PaymentBookingSuccessEvent bookingSuccessEvent = new PaymentBookingSuccessEvent(
                                        txn.getId(),
                                        txn.getBookingId(),
                                        txn.getShowtimeId(),
                                        txn.getUserId(),
                                        txn.getAmount(),
                                        "ZALOPAY",
                                        txn.getSeatIds(),
                                        "Payment confirmed via ZaloPay Callback");
                        paymentProducer.sendPaymentBookingSuccessEvent(bookingSuccessEvent);
                }

                // Nếu là FnB order, gửi event riêng cho FnB Service và tích điểm loyalty
                if (txn.getFnbOrderId() != null) {
                        // Tích điểm loyalty cho FnB: 10,000 VND = 1 điểm
                        java.math.BigDecimal divisor = new java.math.BigDecimal("10000");
                        int pointsEarned = txn.getAmount().divide(divisor, 0, java.math.RoundingMode.DOWN).intValue();

                        if (pointsEarned > 0) {
                                log.info("💎 Earning {} loyalty points for FnB order {} (amount: {})",
                                                pointsEarned, txn.getFnbOrderId(), txn.getAmount());
                                userProfileClient.updateLoyaltyPoints(txn.getUserId(), pointsEarned);
                        }

                        PaymentFnbSuccessEvent fnbSuccessEvent = new PaymentFnbSuccessEvent(
                                        txn.getId(),
                                        txn.getFnbOrderId(),
                                        txn.getUserId(),
                                        txn.getAmount(),
                                        "ZALOPAY",
                                        "Payment confirmed via ZaloPay Callback");
                        paymentProducer.sendPaymentFnbSuccessEvent(fnbSuccessEvent);
                }
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
                        log.warn("No PENDING transaction found for bookingId {}. Skipping update.",
                                        event.bookingId());
                        return;
                }

                PaymentTransaction txn = optionalTxn.get();
                txn.setAmount(event.finalPrice());
                paymentRepository.save(txn);

                log.info("Updated transaction amount for bookingId {} → {}", event.bookingId(), event.finalPrice());
        }

        @Transactional
        public void updateStatus(SeatUnlockedEvent event) {
                log.info("🕓 Updating payment status due to seat unlock | bookingId={} | reason={}",
                                event.bookingId(), event.reason());

                // Tìm transaction đang PENDING
                Optional<PaymentTransaction> optionalTxn = paymentRepository.findByBookingId(event.bookingId())
                                .stream()
                                .filter(t -> t.getStatus() == PaymentStatus.PENDING)
                                .findFirst();

                if (optionalTxn.isEmpty()) {
                        log.warn("No PENDING transaction found for bookingId {}. Skipping status update.",
                                        event.bookingId());
                        return;
                }

                PaymentTransaction txn = optionalTxn.get();

                // ✅ Cập nhật trạng thái sang EXPIRED
                txn.setStatus(PaymentStatus.EXPIRED);
                txn.setTransactionRef("TXN_EXPIRED_" + UUID.randomUUID());
                paymentRepository.save(txn);

                log.info("💤 Transaction marked as EXPIRED for bookingId {}", event.bookingId());

                // ✅ (Tuỳ chọn) phát event cho booking-service hoặc notification-service
                PaymentBookingFailedEvent expiredEvent = new PaymentBookingFailedEvent(
                                txn.getId(),
                                txn.getBookingId(),
                                txn.getUserId(),
                                txn.getShowtimeId(),
                                txn.getAmount(),
                                txn.getMethod(),
                                txn.getSeatIds(),
                                "Payment expired: " + event.reason());

                paymentProducer.sendPaymentBookingFailedEvent(expiredEvent);
        }

        public List<PaymentTransactionResponse> getPaymentsByUserId(UUID userId) {
                List<PaymentTransaction> payments = paymentRepository.findByUserId(userId);
                return payments.stream()
                                .map(this::toResponse)
                                .toList();
        }

        public PaymentTransactionResponse getPaymentById(UUID id) {
                PaymentTransaction payment = paymentRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
                return toResponse(payment);
        }

        public PaymentTransactionResponse getTransactionByRef(String transactionRef) {
                return paymentRepository.findByTransactionRef(transactionRef)
                                .map(this::toResponse)
                                .orElse(null);
        }

        @Transactional
        public void handlePaymentCancelled(String appTransId, String reason) {
                log.info("❌ Handling payment cancellation for appTransId: {} | reason: {}", appTransId, reason);

                Optional<PaymentTransaction> optionalTxn = paymentRepository.findByTransactionRef(appTransId);

                if (optionalTxn.isEmpty()) {
                        log.warn("No transaction found for appTransId: {}. Skipping cancellation.", appTransId);
                        return;
                }

                PaymentTransaction txn = optionalTxn.get();

                // Chỉ xử lý nếu transaction đang PENDING
                if (txn.getStatus() != PaymentStatus.PENDING) {
                        log.warn("Transaction {} is not PENDING (current: {}). Skipping cancellation.",
                                        appTransId, txn.getStatus());
                        return;
                }

                // Cập nhật status sang FAILED
                txn.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(txn);

                log.info("💔 Transaction marked as FAILED for appTransId: {}", appTransId);

                // Gửi event để booking-service unlock ghế và cancel booking
                if (txn.getBookingId() != null) {
                        PaymentBookingFailedEvent failedEvent = new PaymentBookingFailedEvent(
                                        txn.getId(),
                                        txn.getBookingId(),
                                        txn.getUserId(),
                                        txn.getShowtimeId(),
                                        txn.getAmount(),
                                        txn.getMethod(),
                                        txn.getSeatIds(),
                                        "Payment cancelled: " + reason);

                        paymentProducer.sendPaymentBookingFailedEvent(failedEvent);
                        log.info("📤 Sent PaymentBookingFailedEvent for bookingId: {}", txn.getBookingId());
                }

                // Xử lý FnB order nếu có
                if (txn.getFnbOrderId() != null) {
                        PaymentFnbFailedEvent fnbFailedEvent = new PaymentFnbFailedEvent(
                                        txn.getId(),
                                        txn.getFnbOrderId(),
                                        txn.getUserId(),
                                        txn.getAmount(),
                                        txn.getMethod(),
                                        "Payment cancelled",
                                        reason);

                        paymentProducer.sendPaymentFnbFailedEvent(fnbFailedEvent);
                        log.info("📤 Sent PaymentFnbFailedEvent for fnbOrderId: {}", txn.getFnbOrderId());
                }
        }

        /**
         * Cancel a pending payment by bookingId when user returns from payment gateway without completing
         */
        @Transactional
        public void cancelPendingPaymentByBookingId(UUID bookingId, String reason) {
                log.info("🔄 Cancelling pending payment for bookingId: {} | reason: {}", bookingId, reason);

                List<PaymentTransaction> transactions = paymentRepository.findByBookingId(bookingId);

                if (transactions.isEmpty()) {
                        log.warn("No transaction found for bookingId: {}. Skipping cancellation.", bookingId);
                        return;
                }

                // Get the latest pending transaction
                PaymentTransaction txn = transactions.stream()
                                .filter(t -> t.getStatus() == PaymentStatus.PENDING)
                                .findFirst()
                                .orElse(null);

                if (txn == null) {
                        log.warn("No PENDING transaction found for bookingId: {}. Skipping cancellation.", bookingId);
                        return;
                }

                // Cập nhật status sang FAILED
                txn.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(txn);

                log.info("💔 Transaction marked as FAILED for bookingId: {}", bookingId);

                // Gửi event để booking-service unlock ghế và cancel booking
                PaymentBookingFailedEvent failedEvent = new PaymentBookingFailedEvent(
                                txn.getId(),
                                txn.getBookingId(),
                                txn.getUserId(),
                                txn.getShowtimeId(),
                                txn.getAmount(),
                                txn.getMethod(),
                                txn.getSeatIds(),
                                "Payment cancelled: " + reason);

                paymentProducer.sendPaymentBookingFailedEvent(failedEvent);
                log.info("📤 Sent PaymentBookingFailedEvent for bookingId: {}", bookingId);
        }

        /**
         * Confirm a free booking (when finalPrice = 0, no payment gateway needed)
         * This directly confirms the booking without going through ZaloPay
         */
        @Transactional
        public void confirmFreeBooking(UUID bookingId) {
                log.info("🎁 Confirming free booking for bookingId: {}", bookingId);

                List<PaymentTransaction> transactions = paymentRepository.findByBookingId(bookingId);

                if (transactions.isEmpty()) {
                        throw new RuntimeException("No transaction found for bookingId: " + bookingId);
                }

                // Get the latest pending transaction
                PaymentTransaction txn = transactions.stream()
                                .filter(t -> t.getStatus() == PaymentStatus.PENDING)
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("No PENDING transaction found for bookingId: " + bookingId));

                // Verify amount is 0
                if (txn.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        throw new RuntimeException("Cannot confirm free booking - amount is not 0: " + txn.getAmount());
                }

                // Update status to SUCCESS
                txn.setStatus(PaymentStatus.SUCCESS);
                txn.setMethod("FREE_VOUCHER");
                txn.setTransactionRef("FREE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                paymentRepository.save(txn);

                log.info("✅ Free booking transaction marked as SUCCESS for bookingId: {}", bookingId);

                // Send success event to booking-service
                PaymentBookingSuccessEvent successEvent = new PaymentBookingSuccessEvent(
                                txn.getId(),
                                txn.getBookingId(),
                                txn.getShowtimeId(),
                                txn.getUserId(),
                                txn.getAmount(),
                                "FREE_VOUCHER",
                                txn.getSeatIds(),
                                "Free booking confirmed - paid with refund voucher");

                paymentProducer.sendPaymentBookingSuccessEvent(successEvent);
                log.info("📤 Sent PaymentBookingSuccessEvent for free bookingId: {}", bookingId);
        }

        public PagedResponse<PaymentTransactionResponse> getPaymentsByCriteria(
                        PaymentCriteria criteria, int page, int size, String sortBy, String sortDir) {

                Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC
                                : Sort.Direction.ASC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

                Page<PaymentTransaction> paymentPage = paymentRepository.findByCriteria(criteria, pageable);

                List<PaymentTransactionResponse> responses = paymentPage.getContent().stream()
                                .map(this::toResponse)
                                .toList();

                return PagedResponse.<PaymentTransactionResponse>builder()
                                .data(responses)
                                .page(page)
                                .size(size)
                                .totalElements(paymentPage.getTotalElements())
                                .totalPages(paymentPage.getTotalPages())
                                .build();
        }

        private PaymentTransactionResponse toResponse(PaymentTransaction txn) {
                return PaymentTransactionResponse.builder()
                                .id(txn.getId())
                                .bookingId(txn.getBookingId())
                                .userId(txn.getUserId())
                                .showtimeId(txn.getShowtimeId())
                                .seatIds(txn.getSeatIds())
                                .amount(txn.getAmount())
                                .method(txn.getMethod())
                                .status(txn.getStatus())
                                .transactionRef(txn.getTransactionRef())
                                .createdAt(txn.getCreatedAt())
                                .updatedAt(txn.getUpdatedAt())
                                .build();
        }
}