package com.cinehub.showtime.service;

import com.cinehub.showtime.dto.request.TheaterRequest;
import com.cinehub.showtime.dto.response.TheaterResponse;
import com.cinehub.showtime.entity.Province;
import com.cinehub.showtime.entity.Theater;
import com.cinehub.showtime.repository.ProvinceRepository;
import com.cinehub.showtime.repository.TheaterRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TheaterService {
    private final TheaterRepository theaterRepository;
    private final ProvinceRepository provinceRepository;

    public List<TheaterResponse> searchByName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return getAllTheaters();
        }
        return theaterRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::mapToTheaterResponse)
                .collect(Collectors.toList());
    }

    public TheaterResponse createTheater(TheaterRequest request) {
        Province province = provinceRepository.findById(request.getProvinceId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Province with ID " + request.getProvinceId() + " not found"));

        Theater theater = Theater.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .address(request.getAddress())
                .province(province)
                .description(request.getDescription())
                .build();

        Theater savedTheater = theaterRepository.save(theater);
        return mapToTheaterResponse(savedTheater);
    }

    public TheaterResponse getTheaterById(UUID id) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Theater not found"));
        return mapToTheaterResponse(theater);
    }

    public List<TheaterResponse> getAllTheaters() {
        return theaterRepository.findAll().stream()
                .map(t -> mapToTheaterResponse(t)).collect(Collectors.toList());
    }

    public List<TheaterResponse> getTheatersByProvince(UUID provinceId) {
        return theaterRepository.findByProvinceId(provinceId).stream()
                .map(t -> mapToTheaterResponse(t)).collect(Collectors.toList());
    }

    public TheaterResponse updateTheater(UUID id, TheaterRequest request) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Theater not found"));

        Province province = provinceRepository.findById(request.getProvinceId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Province with ID " + request.getProvinceId() + " not found"));

        theater.setName(request.getName());
        theater.setAddress(request.getAddress());
        theater.setProvince(province);
        theater.setDescription(request.getDescription());

        Theater updatedTheater = theaterRepository.save(theater);
        return mapToTheaterResponse(updatedTheater);
    }

    public void deleteTheater(UUID id) {
        theaterRepository.deleteById(id);
    }

    // --- Helper function: Mapping từ Entity sang Response DTO ---
    private TheaterResponse mapToTheaterResponse(Theater theater) {
        return TheaterResponse.builder()
                .id(theater.getId())
                .name(theater.getName())
                .address(theater.getAddress())
                .description(theater.getDescription())
                .provinceName(theater.getProvince().getName())
                .build();
    }
}