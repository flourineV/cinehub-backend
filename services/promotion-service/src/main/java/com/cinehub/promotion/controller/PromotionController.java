package com.cinehub.promotion.controller;

import com.cinehub.promotion.dto.request.PromotionRequest;
import com.cinehub.promotion.dto.response.PromotionResponse;
import com.cinehub.promotion.dto.response.PromotionValidationResponse;
import com.cinehub.promotion.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/validate")
    public ResponseEntity<PromotionValidationResponse> validatePromotion(
            @RequestParam String code) {
        try {
            PromotionValidationResponse response = promotionService.validatePromotionCode(code);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Trả về 404 hoặc 400 tùy theo cách bạn muốn xử lý lỗi mã không hợp lệ
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    @PostMapping
    public ResponseEntity<PromotionResponse> createPromotion(@Valid @RequestBody PromotionRequest request) {
        try {
            PromotionResponse response = promotionService.createPromotion(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            // Xử lý lỗi trùng code (UNIQUE CONSTRAINT)
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponse> updatePromotion(
            @PathVariable UUID id,
            @Valid @RequestBody PromotionRequest request) {
        try {
            PromotionResponse response = promotionService.updatePromotion(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable UUID id) {
        try {
            promotionService.deletePromotion(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}