package com.cinehub.pricing.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.cinehub.pricing.entity.Promotion;
import com.cinehub.pricing.repository.PromotionRepository;

@Service
public class PromotionService {
    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public List<Promotion> findAll() {
        return promotionRepository.findAll();
    }

    public Promotion findById(UUID id) {
        return promotionRepository.findById(id).orElse(null);
    }

    public Promotion save(Promotion promo) {
        return promotionRepository.save(promo);
    }

    public Promotion update(UUID id, Promotion promo) {
        Promotion existing = promotionRepository.findById(id).orElse(null);
        if (existing != null) {
            existing.setPromoCode(promo.getPromoCode());
            existing.setDescription(promo.getDescription());
            existing.setDiscountPercent(promo.getDiscountPercent());
            existing.setStartDate(promo.getStartDate());
            existing.setEndDate(promo.getEndDate());
            return promotionRepository.save(existing);
        }
        return null;
    }

    public void delete(UUID id) {
        promotionRepository.deleteById(id);
    }
}
