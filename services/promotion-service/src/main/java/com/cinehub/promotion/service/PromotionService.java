package com.cinehub.promotion.service;

import com.cinehub.promotion.dto.request.PromotionRequest;
import com.cinehub.promotion.dto.response.PromotionResponse;
import com.cinehub.promotion.dto.response.PromotionValidationResponse;
import com.cinehub.promotion.entity.Promotion;
import com.cinehub.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    // ---------------------------------------------------------------------
    // 1. LOGIC KIỂM TRA MÃ KHUYẾN MÃI (CHO BOOKING SERVICE)
    // ---------------------------------------------------------------------

    /**
     * Kiểm tra mã khuyến mãi và trả về chi tiết nếu hợp lệ.
     * 
     * @param code Mã khuyến mãi.
     * @return PromotionValidationResponse nếu hợp lệ.
     * @throws IllegalArgumentException nếu mã không hợp lệ hoặc đã hết hạn.
     */
    public PromotionValidationResponse validatePromotionCode(String code) {
        LocalDateTime now = LocalDateTime.now();

        // Sử dụng phương thức Query tùy chỉnh để kiểm tra hiệu lực
        Promotion promotion = promotionRepository.findValidPromotionByCode(code, now)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mã khuyến mãi không hợp lệ, không tồn tại hoặc đã hết hạn."));

        return PromotionValidationResponse.builder()
                .code(promotion.getCode())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .isOneTimeUse(promotion.getIsOneTimeUse())
                .build();
    }

    // ---------------------------------------------------------------------
    // 2. CRUD CHO ADMIN/STAFF
    // ---------------------------------------------------------------------

    /**
     * Lấy tất cả các chương trình khuyến mãi.
     */
    public List<PromotionResponse> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Tạo mới một chương trình khuyến mãi.
     */
    @Transactional
    public PromotionResponse createPromotion(PromotionRequest request) {
        if (promotionRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Mã khuyến mãi đã tồn tại.");
        }

        Promotion newPromo = mapToEntity(request);
        Promotion savedPromo = promotionRepository.save(newPromo);
        log.info("⭐ Created new promotion: {}", savedPromo.getCode());
        return mapToResponse(savedPromo);
    }

    /**
     * Cập nhật chương trình khuyến mãi.
     */
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

        Promotion updatedPromo = promotionRepository.save(existingPromo);
        log.info("🔄 Updated promotion: {}", updatedPromo.getCode());
        return mapToResponse(updatedPromo);
    }

    /**
     * Xóa chương trình khuyến mãi.
     */
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
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(request.getIsActive())
                .isOneTimeUse(request.getIsOneTimeUse())
                .description(request.getDescription())
                .build();
    }

    private PromotionResponse mapToResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .isActive(promotion.getIsActive())
                .isOneTimeUse(promotion.getIsOneTimeUse())
                .description(promotion.getDescription())
                .build();
    }
}