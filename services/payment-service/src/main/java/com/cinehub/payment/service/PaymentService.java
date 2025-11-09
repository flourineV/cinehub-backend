package com.cinehub.payment.service;

import com.cinehub.payment.entity.PaymentTransaction;
import com.cinehub.payment.entity.PaymentStatus;
import com.cinehub.payment.events.BookingCreatedEvent;
import com.cinehub.payment.events.BookingFinalizedEvent;
import com.cinehub.payment.events.BookingRefundedEvent;
import com.cinehub.payment.events.PaymentSuccessEvent;
import com.cinehub.payment.events.SeatUnlockedEvent;
import com.cinehub.payment.events.PaymentFailedEvent;
import com.cinehub.payment.producer.PaymentProducer;
import com.cinehub.payment.repository.PaymentRepository;
import com.cinehub.payment.exception.PaymentProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

        private final PaymentProducer paymentProducer;
        private final PaymentRepository paymentRepository;

        @Transactional
        public void createPendingTransaction(BookingCreatedEvent event) {
                PaymentTransaction pendingTxn = PaymentTransaction.builder()
                                .bookingId(event.bookingId())
                                .userId(event.userId())
                                .showtimeId(event.showtimeId())
                                .seatIds(event.seatIds())
                                .amount(event.totalPrice())
                                .method("INIT_GATEWAY")
                                .status(PaymentStatus.PENDING)
                                .transactionRef("TXN_PENDING_" + UUID.randomUUID().toString())
                                .build();

                paymentRepository.save(pendingTxn);
                log.info("PENDING Transaction created for bookingId: {}", event.bookingId());
        }

        @Transactional
        public void processPaymentSuccess(UUID bookingId, String transactionRef, String paymentMethod) {

                Optional<PaymentTransaction> optionalTxn = paymentRepository.findByBookingId(bookingId)
                                .stream()
                                .filter(t -> t.getStatus() == PaymentStatus.PENDING)
                                .findFirst();

                if (optionalTxn.isEmpty()) {
                        log.error("Transaction not found or not PENDING for bookingId {}. Cannot confirm payment.",
                                        bookingId);
                        throw new PaymentProcessingException(
                                        "Transaction not found or not PENDING for bookingId: " + bookingId);
                }

                PaymentTransaction txn = optionalTxn.get();

                if (txn.getStatus() == PaymentStatus.SUCCESS) {
                        log.warn("Transaction for bookingId {} already SUCCESS. Skipping.", bookingId);
                        return;
                }

                txn.setStatus(PaymentStatus.SUCCESS);
                txn.setTransactionRef(transactionRef);
                txn.setMethod(paymentMethod);
                paymentRepository.save(txn);
                log.info("SUCCESS: Payment transaction updated for bookingId: {}", bookingId);

                PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                                txn.getId(),
                                txn.getBookingId(),
                                txn.getShowtimeId(),
                                txn.getUserId(),
                                txn.getAmount(),
                                txn.getMethod(),
                                txn.getSeatIds(),
                                "PAYMENT_SUCCESS");

                paymentProducer.sendPaymentSuccessEvent(successEvent);
        }

        @Transactional
        public void processPaymentFailure(UUID bookingId, String transactionRef, String reason) {

                Optional<PaymentTransaction> optionalTxn = paymentRepository.findByBookingId(bookingId)
                                .stream()
                                .filter(t -> t.getStatus() == PaymentStatus.PENDING)
                                .findFirst();

                if (optionalTxn.isEmpty()) {
                        log.error("Transaction not found or not PENDING for bookingId {}. Cannot record failure.",
                                        bookingId);
                        throw new PaymentProcessingException(
                                        "Transaction not found or not PENDING for bookingId: " + bookingId);
                }

                PaymentTransaction txn = optionalTxn.get();

                txn.setStatus(PaymentStatus.FAILED);
                txn.setTransactionRef(transactionRef);
                paymentRepository.save(txn);
                log.warn("FAILED: Payment transaction updated for bookingId: {}", bookingId);

                PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                                txn.getId(),
                                txn.getBookingId(),
                                txn.getUserId(),
                                txn.getShowtimeId(),
                                txn.getAmount(),
                                txn.getMethod(),
                                txn.getSeatIds(),
                                reason);

                paymentProducer.sendPaymentFailedEvent(failedEvent);
        }

        @Transactional
        public void processRefund(BookingRefundedEvent event) {
                log.info("Processing refund for bookingId={} | refundedValue={}",
                                event.bookingId(), event.refundedValue());

                Optional<PaymentTransaction> optionalTxn = paymentRepository.findByBookingId(event.bookingId())
                                .stream()
                                .filter(t -> t.getStatus() == PaymentStatus.SUCCESS)
                                .findFirst();

                if (optionalTxn.isEmpty()) {
                        log.error("No SUCCESS transaction found for bookingId {}. Cannot process refund.",
                                        event.bookingId());
                        throw new PaymentProcessingException(
                                        "No SUCCESS transaction found for bookingId: " + event.bookingId());
                }

                PaymentTransaction txn = optionalTxn.get();

                txn.setStatus(PaymentStatus.REFUNDED);
                txn.setTransactionRef("TXN_REFUND_" + UUID.randomUUID());
                txn.setAmount(event.refundedValue());
                paymentRepository.save(txn);

                log.info("REFUNDED: Transaction updated for bookingId {} | amount={}",
                                event.bookingId(), event.refundedValue());

                PaymentSuccessEvent refundEvent = new PaymentSuccessEvent(
                                txn.getId(),
                                txn.getBookingId(),
                                txn.getShowtimeId(),
                                txn.getUserId(),
                                txn.getAmount(),
                                txn.getMethod(),
                                txn.getSeatIds(),
                                "PAYMENT_REFUNDED");

                paymentProducer.sendPaymentSuccessEvent(refundEvent);
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
                PaymentFailedEvent expiredEvent = new PaymentFailedEvent(
                                txn.getId(),
                                txn.getBookingId(),
                                txn.getUserId(),
                                txn.getShowtimeId(),
                                txn.getAmount(),
                                txn.getMethod(),
                                txn.getSeatIds(),
                                "Payment expired: " + event.reason());

                paymentProducer.sendPaymentFailedEvent(expiredEvent);
        }

}