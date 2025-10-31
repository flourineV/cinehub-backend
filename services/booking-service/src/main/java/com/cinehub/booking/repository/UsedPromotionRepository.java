package com.cinehub.booking.repository;

import com.cinehub.booking.entity.UsedPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface UsedPromotionRepository extends JpaRepository<UsedPromotion, UUID> {

    Optional<UsedPromotion> findByUserIdAndPromotionCode(UUID userId, String promotionCode);

    @Transactional
    void deleteByBooking_Id(UUID bookingId);

    boolean existsByUserIdAndPromotionCode(UUID userId, String promotionCode);
}