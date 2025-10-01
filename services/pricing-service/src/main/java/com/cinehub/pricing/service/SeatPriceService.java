package com.cinehub.pricing.service;

import com.cinehub.pricing.dto.request.SeatPriceRequest;
import com.cinehub.pricing.dto.response.SeatPriceResponse;
import com.cinehub.pricing.entity.SeatPrice;
import com.cinehub.pricing.repository.SeatPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatPriceService {

    private final SeatPriceRepository seatPriceRepository;

    public SeatPriceResponse getPriceBySeatType(String seatType) {
        SeatPrice seatPrice = seatPriceRepository.findBySeatType(seatType.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Seat type not found: " + seatType));

        return mapToResponse(seatPrice);
    }

    public List<SeatPriceResponse> getAllSeatPrices() {
        return seatPriceRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SeatPriceResponse addSeatPrice(SeatPriceRequest request) {
        SeatPrice seatPrice = SeatPrice.builder()
                .seatType(request.getSeatType().toUpperCase())
                .basePrice(request.getBasePrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .build();
        seatPriceRepository.save(seatPrice);
        return mapToResponse(seatPrice);
    }

    // helper function
    private SeatPriceResponse mapToResponse(SeatPrice seatPrice) {
        return SeatPriceResponse.builder()
                .seatType(seatPrice.getSeatType())
                .basePrice(seatPrice.getBasePrice())
                .currency(seatPrice.getCurrency())
                .build();
    }
}
