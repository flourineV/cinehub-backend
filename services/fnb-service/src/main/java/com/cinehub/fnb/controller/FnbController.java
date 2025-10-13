package com.cinehub.fnb.controller;

import com.cinehub.fnb.dto.request.FnbCalculationRequest;
import com.cinehub.fnb.dto.request.FnbItemRequest;
import com.cinehub.fnb.dto.response.FnbCalculationResponse;
import com.cinehub.fnb.dto.response.FnbItemResponse;
import com.cinehub.fnb.service.FnbService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fnb")
@RequiredArgsConstructor
public class FnbController {

    private final FnbService fnbService;

    // ---------------------------------------------------------------------
    // 1. API TÍNH TOÁN (CHO BOOKING SERVICE)
    // ---------------------------------------------------------------------

    /**
     * POST /api/fnb/calculate
     * Endpoint được Booking Service gọi để tính tổng giá trị F&B.
     */
    @PostMapping("/calculate")
    public ResponseEntity<FnbCalculationResponse> calculateFnbPrice(
            @Valid @RequestBody FnbCalculationRequest request) {

        FnbCalculationResponse response = fnbService.calculateTotalPrice(request.getSelectedFnbItems());
        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------------------
    // 2. API QUẢN LÝ (CHO STAFF/ADMIN CRUD)
    // ---------------------------------------------------------------------

    /**
     * GET /api/fnb - Lấy tất cả các mục F&B
     */
    @GetMapping
    public ResponseEntity<List<FnbItemResponse>> getAllFnbItems() {
        return ResponseEntity.ok(fnbService.getAllFnbItems());
    }

    /**
     * POST /api/fnb - Tạo mục F&B mới
     */
    @PostMapping
    public ResponseEntity<FnbItemResponse> createFnbItem(@Valid @RequestBody FnbItemRequest request) {
        try {
            FnbItemResponse response = fnbService.createFnbItem(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            // Xử lý lỗi trùng tên (UNIQUE CONSTRAINT)
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * PUT /api/fnb/{id} - Cập nhật mục F&B
     */
    @PutMapping("/{id}")
    public ResponseEntity<FnbItemResponse> updateFnbItem(
            @PathVariable UUID id,
            @Valid @RequestBody FnbItemRequest request) {
        try {
            FnbItemResponse response = fnbService.updateFnbItem(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * DELETE /api/fnb/{id} - Xóa mục F&B
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFnbItem(@PathVariable UUID id) {
        try {
            fnbService.deleteFnbItem(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}