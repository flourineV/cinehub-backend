package com.cinehub.payment.repository;

import com.cinehub.payment.entity.PaymentTransaction;
import com.cinehub.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);

    List<PaymentTransaction> findByUserId(UUID userId);

    List<PaymentTransaction> findByBookingId(UUID bookingId);

    List<PaymentTransaction> findByStatus(PaymentStatus status);
}
