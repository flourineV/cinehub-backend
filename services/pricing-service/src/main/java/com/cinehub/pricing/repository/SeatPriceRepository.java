package com.cinehub.pricing.repository;

import com.cinehub.pricing.entity.SeatPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SeatPriceRepository extends JpaRepository<SeatPrice, UUID> {

    /**
     * Tra cứu mức giá cụ thể dựa trên loại ghế và loại vé.
     * Đây là phương thức cốt lõi cho Pricing Service.
     */
    Optional<SeatPrice> findBySeatTypeAndTicketType(String seatType, String ticketType);
}