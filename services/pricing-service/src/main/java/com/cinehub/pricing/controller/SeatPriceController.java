package com.cinehub.pricing.controller;

import com.cinehub.pricing.dto.request.SeatPriceRequest;
import com.cinehub.pricing.dto.response.SeatPriceResponse;
import com.cinehub.pricing.service.SeatPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/prices/seats")
@RequiredArgsConstructor
public class SeatPriceController {

    private final SeatPriceService seatPriceService;

    @GetMapping("/{seatType}")
    public SeatPriceResponse getPrice(@PathVariable String seatType) {
        return seatPriceService.getPriceBySeatType(seatType);
    }

    @GetMapping
    public List<SeatPriceResponse> getAll() {
        return seatPriceService.getAllSeatPrices();
    }

    @PostMapping
    public SeatPriceResponse addPrice(@RequestBody SeatPriceRequest request) {
        return seatPriceService.addSeatPrice(request);
    }
}
