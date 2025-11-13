package com.cinehub.pricing.controller;

import com.cinehub.pricing.dto.request.SeatPriceRequest;
import com.cinehub.pricing.dto.response.SeatPriceResponse;
import com.cinehub.pricing.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @GetMapping("/seat-price")
    public ResponseEntity<SeatPriceResponse> getSeatPrice(
            @RequestParam @NotBlank String seatType,
            @RequestParam @NotBlank String ticketType) {

        SeatPriceResponse response = pricingService.getSeatBasePrice(seatType, ticketType);

        if (response == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------------------
    // ⬇️ 2. API QUẢN LÝ (STAFF/ADMIN) ⬇️
    // ---------------------------------------------------------------------

    /**
     * GET /api/pricing (Lấy tất cả các mức giá)
     */
    @GetMapping
    public ResponseEntity<List<SeatPriceResponse>> getAllSeatPrices() {
        List<SeatPriceResponse> prices = pricingService.getAllSeatPrices();
        return ResponseEntity.ok(prices);
    }

    /**
     * POST /api/pricing (Tạo mức giá mới)
     */
    @PostMapping
    public ResponseEntity<SeatPriceResponse> createSeatPrice(@Valid @RequestBody SeatPriceRequest request) {
        try {
            SeatPriceResponse response = pricingService.createSeatPrice(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            // Xử lý lỗi khi đã tồn tại (UNIQUE CONSTRAINT)
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * PUT /api/pricing/{id} (Cập nhật mức giá)
     */
    @PutMapping("/{id}")
    public ResponseEntity<SeatPriceResponse> updateSeatPrice(
            @PathVariable UUID id,
            @Valid @RequestBody SeatPriceRequest request) {
        try {
            SeatPriceResponse response = pricingService.updateSeatPrice(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * DELETE /api/pricing/{id} (Xóa mức giá)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeatPrice(@PathVariable UUID id) {
        try {
            pricingService.deleteSeatPrice(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}