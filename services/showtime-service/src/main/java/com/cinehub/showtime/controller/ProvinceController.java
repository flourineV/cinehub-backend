package com.cinehub.showtime.controller;

import com.cinehub.showtime.dto.request.ProvinceRequest;
import com.cinehub.showtime.dto.response.ProvinceResponse;
import com.cinehub.showtime.security.AuthChecker;
import com.cinehub.showtime.service.ProvinceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes/provinces")
@RequiredArgsConstructor
public class ProvinceController {

    private final ProvinceService provinceService;

    @PostMapping
    public ResponseEntity<ProvinceResponse> createProvince(@RequestBody ProvinceRequest request) {
        AuthChecker.requireManagerOrAdmin();
        ProvinceResponse response = provinceService.createProvince(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProvinceResponse> getProvinceById(@PathVariable UUID id) {
        return ResponseEntity.ok(provinceService.getProvinceById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProvinceResponse>> getAllProvinces() {
        return ResponseEntity.ok(provinceService.getAllProvinces());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProvinceResponse> updateProvince(
            @PathVariable UUID id,
            @RequestBody ProvinceRequest request) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(provinceService.updateProvince(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProvince(@PathVariable UUID id) {
        AuthChecker.requireManagerOrAdmin();
        provinceService.deleteProvince(id);
        return ResponseEntity.noContent().build();
    }
}
