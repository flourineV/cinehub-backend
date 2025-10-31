package com.cinehub.booking.repository;

import com.cinehub.booking.entity.BookingFnb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

public interface BookingFnbRepository extends JpaRepository<BookingFnb, UUID> {

    @Transactional
    void deleteByBooking_Id(UUID bookingId);

    List<BookingFnb> findByBooking_Id(UUID bookingId);
}