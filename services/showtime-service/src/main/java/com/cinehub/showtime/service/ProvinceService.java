package com.cinehub.showtime.service;

import com.cinehub.showtime.dto.request.ProvinceRequest;
import com.cinehub.showtime.dto.response.ProvinceResponse;
import com.cinehub.showtime.entity.Province;
import com.cinehub.showtime.repository.ProvinceRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class ProvinceService {
    private final ProvinceRepository provinceRepository;

    public ProvinceResponse createProvince(ProvinceRequest request) {

        Province province = Province.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .build();

        Province savedProvince = provinceRepository.save(province);
        return new ProvinceResponse(savedProvince.getId(), savedProvince.getName());
    }

    public ProvinceResponse getProvinceById(UUID id) {
        Province province = provinceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Province with ID " + id + " not found"));
        return new ProvinceResponse(province.getId(), province.getName());
    }

    public List<ProvinceResponse> getAllProvinces() {
        return provinceRepository.findAll().stream().map(p -> new ProvinceResponse(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }

    public ProvinceResponse updateProvince(UUID id, ProvinceRequest request) {
        Province province = provinceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Province not found"));
        province.setName(request.getName());
        provinceRepository.save(province);
        return new ProvinceResponse(province.getId(), province.getName());
    }

    public void deleteProvince(UUID id) {
        provinceRepository.deleteById(id);
    }
}
