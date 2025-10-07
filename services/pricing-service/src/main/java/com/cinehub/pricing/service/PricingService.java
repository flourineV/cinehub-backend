package com.cinehub.pricing.service;

import com.cinehub.pricing.dto.request.PricingRequest;
import com.cinehub.pricing.entity.Combo;
import com.cinehub.pricing.entity.Promotion;
import com.cinehub.pricing.repository.ComboRepository;
import com.cinehub.pricing.repository.PromotionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class PricingService {

    private final ComboRepository comboRepository;
    private final PromotionRepository promoRepository;

    public PricingService(ComboRepository comboRepository, PromotionRepository promoRepository) {
        this.comboRepository = comboRepository;
        this.promoRepository = promoRepository;
    }

    public double calculateTotal(PricingRequest req) {
        double total = 0;
        // Cộng giá tất cả combo
        for (UUID comboId : req.getItemIds()) {
            total += comboRepository.findById(comboId)
                    .map(Combo::getPrice)
                    .orElse(0.0);
        }

        // Áp mã khuyến mãi (nếu có)
        if (req.getPromoCode() != null && !req.getPromoCode().isEmpty()) {
            Promotion promo = promoRepository.findAll().stream()
                    .filter(p -> p.getPromoCode().equalsIgnoreCase(req.getPromoCode()))
                    .findFirst()
                    .orElse(null);

            if (promo != null
                    && LocalDate.now().isAfter(promo.getStartDate())
                    && LocalDate.now().isBefore(promo.getEndDate())) {
                total = total * (1 - promo.getDiscountPercent() / 100);
            }
        }

        return total;
    }
}
