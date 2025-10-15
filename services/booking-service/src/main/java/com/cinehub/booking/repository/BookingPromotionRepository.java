package com.cinehub.booking.repository;

import com.cinehub.booking.entity.BookingPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

public interface BookingPromotionRepository extends JpaRepository<BookingPromotion, UUID> {

    @Transactional
    void deleteByBooking_Id(UUID bookingId);

    Optional<BookingPromotion> findByBooking_Id(UUID bookingId);
}