package com.cinehub.pricing.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import com.cinehub.pricing.entity.Promotion;
import com.cinehub.pricing.service.PromotionService;

@RestController
@RequestMapping("/api/pricing/promotions")
public class PromotionController {
    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public List<Promotion> getAll() {
        return promotionService.findAll();
    }

    @GetMapping("/{id}")
    public Promotion getById(@PathVariable UUID id) {
        return promotionService.findById(id);
    }

    @GetMapping("/active")
    public List<Promotion> getActivePromotions() {
        return promotionService.findActivePromotions();
    }

    @PostMapping
    public Promotion create(@RequestBody Promotion promo) {
        return promotionService.save(promo);
    }

    @PutMapping("/{id}")
    public Promotion update(@PathVariable UUID id, @RequestBody Promotion promo) {
        return promotionService.update(id, promo);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        promotionService.delete(id);
    }
}
