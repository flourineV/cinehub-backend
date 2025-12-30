package com.cinehub.promotion.service;

import com.cinehub.promotion.dto.request.RecordPromotionUsageRequest;
import com.cinehub.promotion.dto.request.UpdatePromotionUsageStatusRequest;
import com.cinehub.promotion.dto.response.UsedPromotionResponse;
import com.cinehub.promotion.entity.Promotion;
import com.cinehub.promotion.entity.UsedPromotion;
import com.cinehub.promotion.repository.PromotionRepository;
import com.cinehub.promotion.repository.UsedPromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsedPromotionService {

    private final UsedPromotionRepository usedPromotionRepository;
    private final PromotionRepository promotionRepository;

    /**
     * Check if user can use this promotion code
     */
    public boolean canUsePromotion(UUID userId, String promotionCode) {
        // Find promotion
        Optional<Promotion> promotionOpt = promotionRepository.findByCode(promotionCode);
        if (promotionOpt.isEmpty()) {
            return false;
        }

        Promotion promotion = promotionOpt.get();
        
        // If not one-time-use, always allow
        if (!promotion.isOneTimeUse()) {
            return true;
        }

        // Check if user has used this promotion before (and booking is confirmed)
        Optional<UsedPromotion> existingUsage = usedPromotionRepository
                .findByUserIdAndPromotionCode(userId, promotionCode);

        // If no record exists, user can use it
        // If record exists, it means a confirmed booking used it, so can't reuse
        return existingUsage.isEmpty();
    }

    /**
     * Record promotion usage when booking is created
     */
    @Transactional
    public UsedPromotionResponse recordPromotionUsage(RecordPromotionUsageRequest request) {
        // Check if already exists (shouldn't happen with new logic, but safety check)
        Optional<UsedPromotion> existing = usedPromotionRepository
                .findByUserIdAndPromotionCode(request.getUserId(), request.getPromotionCode());

        if (existing.isPresent()) {
            throw new IllegalStateException("User has already used this promotion code");
        }

        UsedPromotion usedPromotion = UsedPromotion.builder()
                .userId(request.getUserId())
                .promotionCode(request.getPromotionCode())
                .bookingId(request.getBookingId())
                .build();

        UsedPromotion saved = usedPromotionRepository.save(usedPromotion);
        log.info("Recorded promotion usage: userId={}, code={}, bookingId={}", 
                request.getUserId(), request.getPromotionCode(), request.getBookingId());

        return mapToResponse(saved);
    }

    /**
     * Handle booking status changes - delete record if booking failed
     */
    @Transactional
    public void updateBookingStatus(UpdatePromotionUsageStatusRequest request) {
        Optional<UsedPromotion> usedPromotionOpt = usedPromotionRepository
                .findByBookingId(request.getBookingId());

        if (usedPromotionOpt.isEmpty()) {
            log.warn("No promotion usage found for bookingId: {}", request.getBookingId());
            return;
        }

        String status = request.getBookingStatus();
        
        // Delete record if booking failed (cancelled, expired, refunded)
        // Keep record only if booking is confirmed
        if ("CANCELLED".equals(status) || "EXPIRED".equals(status) || "REFUNDED".equals(status)) {
            usedPromotionRepository.delete(usedPromotionOpt.get());
            log.info("Deleted promotion usage record due to booking failure: bookingId={}, status={}", 
                    request.getBookingId(), status);
        } else if ("CONFIRMED".equals(status)) {
            log.info("Keeping promotion usage record for confirmed booking: bookingId={}", 
                    request.getBookingId());
        }
        // For PENDING/AWAITING_PAYMENT, do nothing - keep record as is
    }

    /**
     * Get promotion usage by booking ID
     */
    public Optional<UsedPromotionResponse> getByBookingId(UUID bookingId) {
        return usedPromotionRepository.findByBookingId(bookingId)
                .map(this::mapToResponse);
    }

    private UsedPromotionResponse mapToResponse(UsedPromotion entity) {
        return UsedPromotionResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .promotionCode(entity.getPromotionCode())
                .bookingId(entity.getBookingId())
                .usedAt(entity.getUsedAt())
                .build();
    }
}
