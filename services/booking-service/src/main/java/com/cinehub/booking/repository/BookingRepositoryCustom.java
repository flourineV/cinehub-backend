package com.cinehub.booking.repository;

import com.cinehub.booking.dto.request.BookingCriteria;
import com.cinehub.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingRepositoryCustom {
    Page<Booking> searchWithCriteria(BookingCriteria criteria, Pageable pageable);
}
