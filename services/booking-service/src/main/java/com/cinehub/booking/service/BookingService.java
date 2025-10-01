package com.cinehub.booking.service;

import com.cinehub.booking.entity.Booking;
import com.cinehub.booking.repository.BookingRepository;
import com.cinehub.booking.repository.BookingSeatRepository;

import com.cinehub.booking.dto.BookingRequest;
import com.cinehub.booking.dto.BookingUpdateRequest;
import com.cinehub.booking.dto.BookingResponse;
import com.cinehub.booking.dto.BookingSeatResponse;

import com.cinehub.booking.client.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowtimeClient showtimeClient;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        ShowtimeResponse showtime = showtimeClient.getShowtimeById(request.getShowtimeId());
        if (showtime == null) {
            throw new RuntimeException("Showtime not found: " + request.getShowtimeId());
        }

        BigDecimal pricePerSeat = showtime.getPrice();
    }

}
