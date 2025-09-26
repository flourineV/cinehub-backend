package com.cinehub.showtime.controller;

import com.cinehub.showtime.service.ProvinceService;
import com.cinehub.showtime.dto.request.ProvinceRequest;
import com.cinehub.showtime.dto.response.ProvinceResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/provinces")
@RequiredArgsConstructor

public class ProvinceController {
    private final ProvinceService provinceService;

    @PostMapping
    public ResponseEntity<ProvinceResponse> createProvince(@RequestBody ProvinceRequest request){
        ProvinceResponse response = provinceService.createProvince(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProvinceResponse> getProvinceById(@PathVariable String id){
        return ResponseEntity.ok(provinceService.getProvinceById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProvinceResponse>> getAllProvinces(){
        return ResponseEntity.ok(provinceService.getAllProvinces());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProvinceResponse> updateProvince(@RequestBody String id, @RequestBody ProvinceRequest request){
        return ResponseEntity.ok(provinceService.updateProvince(id, request));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteProvince(@PathVariable String id){
        provinceService.deleteProvince(id);
        return ResponseEntity.noContent().build();
    }
}
