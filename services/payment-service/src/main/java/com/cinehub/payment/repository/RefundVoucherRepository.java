package com.cinehub.payment.repository;

import com.cinehub.payment.entity.RefundVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefundVoucherRepository extends JpaRepository<RefundVoucher, UUID> {

    Optional<RefundVoucher> findByCode(String code);

    Optional<RefundVoucher> findByUserIdAndIsUsedFalse(UUID userId);

    boolean existsByCode(String code);

    long countByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

}
