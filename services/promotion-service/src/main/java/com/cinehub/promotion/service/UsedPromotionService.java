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

        // Check if user has used this promotion before
        Optional<UsedPromotion> existingUsage = usedPromotionRepository
                .findByUserIdAndPromotionCode(userId, promotionCode);

        if (existingUsage.isEmpty()) {
            return true; // Never used before
        }

        // Check booking status
        UsedPromotion.BookingStatus status = existingUsage.get().getBookingStatus();
        
        // Allow reuse if previous booking was cancelled, expired, or refunded
        return status == UsedPromotion.BookingStatus.CANCELLED ||
               status == UsedPromotion.BookingStatus.EXPIRED ||
               status == UsedPromotion.BookingStatus.REFUNDED;
    }

    /**
     * Record promotion usage when booking is created
     */
    @Transactional
    public UsedPromotionResponse recordPromotionUsage(RecordPromotionUsageRequest request) {
        // Check if already exists
        Optional<UsedPromotion> existing = usedPromotionRepository
                .findByUserIdAndPromotionCode(request.getUserId(), request.getPromotionCode());

        if (existing.isPresent()) {
            // Delete old record if booking was cancelled/expired/refunded
            UsedPromotion.BookingStatus status = existing.get().getBookingStatus();
            if (status == UsedPromotion.BookingStatus.CANCELLED ||
                status == UsedPromotion.BookingStatus.EXPIRED ||
                status == UsedPromotion.BookingStatus.REFUNDED) {
                usedPromotionRepository.delete(existing.get());
                log.info("Deleted old promotion usage record for reuse: userId={}, code={}", 
                        request.getUserId(), request.getPromotionCode());
            } else {
                throw new IllegalStateException("User has already used this promotion code");
            }
        }

        UsedPromotion usedPromotion = UsedPromotion.builder()
                .userId(request.getUserId())
                .promotionCode(request.getPromotionCode())
                .bookingId(request.getBookingId())
                .bookingStatus(UsedPromotion.BookingStatus.PENDING)
                .build();

        UsedPromotion saved = usedPromotionRepository.save(usedPromotion);
        log.info("Recorded promotion usage: userId={}, code={}, bookingId={}", 
                request.getUserId(), request.getPromotionCode(), request.getBookingId());

        return mapToResponse(saved);
    }

    /**
     * Update booking status when booking status changes
     */
    @Transactional
    public void updateBookingStatus(UpdatePromotionUsageStatusRequest request) {
        Optional<UsedPromotion> usedPromotionOpt = usedPromotionRepository
                .findByBookingId(request.getBookingId());

        if (usedPromotionOpt.isEmpty()) {
            log.warn("No promotion usage found for bookingId: {}", request.getBookingId());
            return;
        }

        UsedPromotion usedPromotion = usedPromotionOpt.get();
        usedPromotion.setBookingStatus(request.getBookingStatus());
        usedPromotionRepository.save(usedPromotion);

        log.info("Updated promotion usage status: bookingId={}, status={}", 
                request.getBookingId(), request.getBookingStatus());
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
                .bookingStatus(entity.getBookingStatus())
                .usedAt(entity.getUsedAt())
                .build();
    }
}
