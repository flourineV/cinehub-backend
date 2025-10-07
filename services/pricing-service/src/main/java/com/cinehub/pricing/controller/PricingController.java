package com.cinehub.pricing.controller;

import com.cinehub.pricing.dto.request.PricingRequest;
import com.cinehub.pricing.service.PricingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/calculate")
    public double calculatePrice(@RequestBody PricingRequest req) {
        return pricingService.calculateTotal(req);
    }
}
