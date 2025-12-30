package com.cinehub.promotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinehub.promotion.entity.RefundVoucher;
import com.cinehub.promotion.entity.RefundVoucher.RefundType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundVoucherRepository extends JpaRepository<RefundVoucher, UUID> {

    Optional<RefundVoucher> findByCode(String code);

    Optional<RefundVoucher> findByUserIdAndIsUsedFalse(UUID userId);

    List<RefundVoucher> findByUserId(UUID userId);

    boolean existsByCode(String code);

    long countByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndRefundTypeAndCreatedAtBetween(UUID userId, RefundType refundType, LocalDateTime start, LocalDateTime end);

}
