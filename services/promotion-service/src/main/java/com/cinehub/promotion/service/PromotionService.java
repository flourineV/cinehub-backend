package com.cinehub.promotion.service;

import com.cinehub.promotion.dto.request.PromotionRequest;
import com.cinehub.promotion.dto.response.PromotionResponse;
import com.cinehub.promotion.dto.response.PromotionValidationResponse;
import com.cinehub.promotion.dto.response.UserPromotionsResponse;
import com.cinehub.promotion.entity.Promotion;
import com.cinehub.promotion.repository.PromotionRepository;
import com.cinehub.promotion.repository.UsedPromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final UsedPromotionRepository usedPromotionRepository;
    private final PromotionNotificationHelper notificationHelper;

    public PromotionValidationResponse validatePromotionCode(String code) {
        LocalDateTime now = LocalDateTime.now();

        // Sử dụng phương thức Query tùy chỉnh để kiểm tra hiệu lực
        Promotion promotion = promotionRepository.findValidPromotionByCode(code, now)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mã khuyến mãi không hợp lệ, không tồn tại hoặc đã hết hạn."));

        // Validate time restriction
        if (!isValidTimeRestriction(promotion, now)) {
            throw new IllegalArgumentException(
                    "Mã khuyến mãi không áp dụng cho thời điểm hiện tại.");
        }

        return PromotionValidationResponse.builder()
                .code(promotion.getCode())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .isOneTimeUse(promotion.isOneTimeUse())
                .build();
    }

    private boolean isValidTimeRestriction(Promotion promotion, LocalDateTime now) {
        if (promotion.getUsageTimeRestriction() == null ||
                promotion.getUsageTimeRestriction() == Promotion.UsageTimeRestriction.NONE) {
            return true;
        }

        java.time.DayOfWeek dayOfWeek = now.getDayOfWeek();
        int dayOfMonth = now.getDayOfMonth();

        switch (promotion.getUsageTimeRestriction()) {
            case WEEKEND_ONLY:
                return dayOfWeek == java.time.DayOfWeek.SATURDAY ||
                        dayOfWeek == java.time.DayOfWeek.SUNDAY;

            case WEEKDAY_ONLY:
                return dayOfWeek != java.time.DayOfWeek.SATURDAY &&
                        dayOfWeek != java.time.DayOfWeek.SUNDAY;

            case CUSTOM_DAYS:
                return isValidCustomDays(promotion, dayOfWeek, dayOfMonth);

            default:
                return true;
        }
    }

    private boolean isValidCustomDays(Promotion promotion, java.time.DayOfWeek dayOfWeek, int dayOfMonth) {
        // Check allowed days of week
        if (promotion.getAllowedDaysOfWeek() != null && !promotion.getAllowedDaysOfWeek().isEmpty()) {
            String[] allowedDays = promotion.getAllowedDaysOfWeek().split(",");
            boolean dayOfWeekMatch = false;
            for (String day : allowedDays) {
                if (dayOfWeek.name().equalsIgnoreCase(day.trim())) {
                    dayOfWeekMatch = true;
                    break;
                }
            }
            if (!dayOfWeekMatch) {
                return false;
            }
        }

        // Check allowed days of month
        if (promotion.getAllowedDaysOfMonth() != null && !promotion.getAllowedDaysOfMonth().isEmpty()) {
            String[] allowedDays = promotion.getAllowedDaysOfMonth().split(",");
            boolean dayOfMonthMatch = false;
            for (String day : allowedDays) {
                try {
                    if (Integer.parseInt(day.trim()) == dayOfMonth) {
                        dayOfMonthMatch = true;
                        break;
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid day of month format: {}", day);
                }
            }
            if (!dayOfMonthMatch) {
                return false;
            }
        }

        return true;
    }

    public List<PromotionResponse> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .filter(p -> p.getIsActive() != null && p.getIsActive())
                .map(this::mapToResponse)
                .toList();
    }

    public List<PromotionResponse> getActivePromotions() {
        return promotionRepository.findAll().stream()
                .filter(p -> p.getIsActive() != null && p.getIsActive())
                .map(this::mapToResponse)
                .toList();
    }

    public UserPromotionsResponse getActivePromotionsForUser(UUID userId) {
        LocalDateTime now = LocalDateTime.now();

        List<Promotion> activePromotions = promotionRepository.findAll().stream()
                .filter(p -> p.getIsActive() != null && p.getIsActive())
                .filter(p -> p.getStartDate() != null && p.getStartDate().isBefore(now))
                .filter(p -> p.getEndDate() != null && p.getEndDate().isAfter(now))
                .toList();

        List<UserPromotionsResponse.ApplicablePromotionResponse> applicable = new ArrayList<>();
        List<UserPromotionsResponse.NotApplicablePromotionResponse> notApplicable = new ArrayList<>();

        for (Promotion promotion : activePromotions) {
            // Check if user already used this promotion (for one-time use)
            if (promotion.isOneTimeUse() &&
                    usedPromotionRepository.existsByUserIdAndPromotionCode(userId, promotion.getCode())) {
                notApplicable.add(UserPromotionsResponse.NotApplicablePromotionResponse.builder()
                        .promotion(mapToResponse(promotion))
                        .build());
                continue;
            }

            // Check time restrictions
            if (!isValidTimeRestriction(promotion, now)) {
                notApplicable.add(UserPromotionsResponse.NotApplicablePromotionResponse.builder()
                        .promotion(mapToResponse(promotion))
                        .build());
                continue;
            }

            // If all checks pass, it's applicable
            applicable.add(UserPromotionsResponse.ApplicablePromotionResponse.builder()
                    .promotion(mapToResponse(promotion))
                    .build());
        }

        return UserPromotionsResponse.builder()
                .applicable(applicable)
                .notApplicable(notApplicable)
                .build();
    }

    public List<PromotionResponse> getAllPromotionsForAdmin(String code, String discountType, String promotionType,
            Boolean isActive) {
        return promotionRepository.findAll().stream()
                .filter(p -> code == null || p.getCode().toLowerCase().contains(code.toLowerCase()))
                .filter(p -> discountType == null || p.getDiscountType().name().equalsIgnoreCase(discountType))
                .filter(p -> promotionType == null || p.getPromotionType().name().equalsIgnoreCase(promotionType))
                .filter(p -> isActive == null || p.getIsActive().equals(isActive))
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public PromotionResponse createPromotion(PromotionRequest request) {
        if (promotionRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Mã khuyến mãi đã tồn tại.");
        }

        Promotion newPromo = mapToEntity(request);
        Promotion savedPromo = promotionRepository.save(newPromo);
        log.info("⭐ Created new promotion: {}", savedPromo.getCode());

        try {
            notificationHelper.sendPromotionNotification(savedPromo);
        } catch (Exception e) {
            log.error("Failed to send promotion notification for {}: {}", savedPromo.getCode(), e.getMessage());
        }

        return mapToResponse(savedPromo);
    }

    @Transactional
    public PromotionResponse updatePromotion(UUID id, PromotionRequest request) {
        Promotion existingPromo = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found with ID: " + id));

        // Kiểm tra xem mã code mới có trùng với mã khác không (nếu mã code bị thay đổi)
        if (!existingPromo.getCode().equals(request.getCode()) &&
                promotionRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Mã khuyến mãi mới đã được sử dụng bởi chương trình khác.");
        }

        existingPromo.setCode(request.getCode());
        existingPromo.setDiscountType(request.getDiscountType());
        existingPromo.setDiscountValue(request.getDiscountValue());
        existingPromo.setStartDate(request.getStartDate());
        existingPromo.setEndDate(request.getEndDate());
        existingPromo.setIsActive(request.getIsActive());
        existingPromo.setDescription(request.getDescription());
        existingPromo.setDescriptionEn(request.getDescriptionEn());
        existingPromo.setPromoDisplayUrl(request.getPromoDisplayUrl());

        Promotion updatedPromo = promotionRepository.save(existingPromo);
        log.info("🔄 Updated promotion: {}", updatedPromo.getCode());
        return mapToResponse(updatedPromo);
    }

    @Transactional
    public void deletePromotion(UUID id) {
        if (!promotionRepository.existsById(id)) {
            throw new IllegalArgumentException("Promotion not found with ID: " + id);
        }
        promotionRepository.deleteById(id);
        log.warn("🗑️ Deleted promotion with ID: {}", id);
    }

    // --- Helper Mappers ---

    private Promotion mapToEntity(PromotionRequest request) {
        return Promotion.builder()
                .code(request.getCode())
                .promotionType(request.getPromotionType() != null ? request.getPromotionType()
                        : Promotion.PromotionType.GENERAL)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(request.getIsActive())
                .usageTimeRestriction(request.getUsageTimeRestriction())
                .allowedDaysOfWeek(request.getAllowedDaysOfWeek())
                .allowedDaysOfMonth(request.getAllowedDaysOfMonth())
                .description(request.getDescription())
                .descriptionEn(request.getDescriptionEn())
                .promoDisplayUrl(request.getPromoDisplayUrl())
                .build();
    }

    private PromotionResponse mapToResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .promotionType(promotion.getPromotionType())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .isActive(promotion.getIsActive())
                .usageTimeRestriction(promotion.getUsageTimeRestriction())
                .allowedDaysOfWeek(promotion.getAllowedDaysOfWeek())
                .allowedDaysOfMonth(promotion.getAllowedDaysOfMonth())
                .description(promotion.getDescription())
                .descriptionEn(promotion.getDescriptionEn())
                .promoDisplayUrl(promotion.getPromoDisplayUrl())
                .build();
    }
}