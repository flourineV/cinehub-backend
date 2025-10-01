package com.cinehub.pricing.repository;

import com.cinehub.pricing.entity.SeatPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SeatPriceRepository extends JpaRepository<SeatPrice, UUID> {
    Optional<SeatPrice> findBySeatType(String seatType);
}
