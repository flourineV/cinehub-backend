package com.cinehub.promotion.controller;

import com.cinehub.promotion.dto.request.RecordPromotionUsageRequest;
import com.cinehub.promotion.dto.request.UpdatePromotionUsageStatusRequest;
import com.cinehub.promotion.dto.response.UsedPromotionResponse;
import com.cinehub.promotion.service.UsedPromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/promotions/usage")
@RequiredArgsConstructor
public class UsedPromotionController {

    private final UsedPromotionService usedPromotionService;

    /**
     * Check if user can use a promotion code (for booking service)
     */
    @GetMapping("/can-use")
    public ResponseEntity<Boolean> canUsePromotion(
            @RequestParam UUID userId,
            @RequestParam String promotionCode,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {
        
        // This endpoint should be called by internal services only
        // Add internal auth check if needed
        
        boolean canUse = usedPromotionService.canUsePromotion(userId, promotionCode);
        return ResponseEntity.ok(canUse);
    }

    /**
     * Record promotion usage when booking is created (for booking service)
     */
    @PostMapping("/record")
    public ResponseEntity<UsedPromotionResponse> recordPromotionUsage(
            @RequestBody RecordPromotionUsageRequest request,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {
        
        // This endpoint should be called by internal services only
        // Add internal auth check if needed
        
        UsedPromotionResponse response = usedPromotionService.recordPromotionUsage(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update booking status (for booking service when status changes)
     */
    @PatchMapping("/update-status")
    public ResponseEntity<Void> updateBookingStatus(
            @RequestBody UpdatePromotionUsageStatusRequest request,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {
        
        // This endpoint should be called by internal services only
        // Add internal auth check if needed
        
        usedPromotionService.updateBookingStatus(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get promotion usage by booking ID
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<UsedPromotionResponse> getByBookingId(
            @PathVariable UUID bookingId,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {
        
        return usedPromotionService.getByBookingId(bookingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
