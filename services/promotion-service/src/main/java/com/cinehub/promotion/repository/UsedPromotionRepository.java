package com.cinehub.promotion.repository;

import com.cinehub.promotion.entity.UsedPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsedPromotionRepository extends JpaRepository<UsedPromotion, UUID> {
    
    Optional<UsedPromotion> findByUserIdAndPromotionCode(UUID userId, String promotionCode);
    
    boolean existsByUserIdAndPromotionCode(UUID userId, String promotionCode);
    
    Optional<UsedPromotion> findByBookingId(UUID bookingId);
}
